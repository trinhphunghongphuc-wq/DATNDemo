package com.service.impl;

import com.dto.blockchain.WalletAnchorDto.*;
import com.entity.Batch;
import com.entity.StageWalletApproval;
import com.entity.User;
import com.enums.RecordStage;
import com.enums.Role;
import com.exception.BusinessException;
import com.exception.ResourceNotFoundException;
import com.repository.BatchRepository;
import com.repository.StageAnchorTransactionRepository;
import com.repository.StageWalletApprovalRepository;
import com.repository.UserRepository;
import com.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.utils.Numeric;

import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class WalletAnchorService {
    private static final String ZERO = "0x0000000000000000000000000000000000000000";
    private static final long APPROVAL_SECONDS = 3600;

    private final BlockchainConnectionService chain;
    private final BatchLockService locks;
    private final BatchRepository batches;
    private final UserRepository users;
    private final StageAnchorTransactionRepository anchors;
    private final StageWalletApprovalRepository approvals;

    @Transactional(readOnly = true)
    public String wallet(CustomUserDetails actor) {
        return account(actor.getId()).getWalletAddress();
    }

    @Transactional
    public String updateWallet(CustomUserDetails actor, String value) {
        String address = walletAddress(value);
        account(actor.getId()).setWalletAddress(address);
        return address;
    }

    @Transactional(readOnly = true)
    public BatchStatus status(Long batchId) {
        Batch batch = findBatch(batchId);
        String mode = batch.getWalletRegistryAddress() != null ? "WALLET"
                : canBind(batch) ? "UNBOUND" : "LEGACY";
        var state = approvals.findByBatchId(batchId).stream().map(approval -> new StageStatus(
                approval.getStage(),
                approval.getDeadline() >= Instant.now().getEpochSecond()
                        && approval.getMerkleRoot().equalsIgnoreCase(rootOrNull(batch, approval.getStage())),
                approval.getWalletAddress())).toList();
        return new BatchStatus(mode, batch.getWalletRegistryAddress(), state);
    }

    @Transactional(rollbackFor = Exception.class)
    public BatchStatus bind(Long batchId) throws Exception {
        Batch batch = locks.lock(batchId);
        if (batch.getWalletRegistryAddress() != null) return status(batchId);
        require(canBind(batch), "Lô đã dùng registry cũ hoặc chưa cấu hình wallet registry.");
        String producer = participantWallet(batch.getCreatedBy() == null ? null : batch.getCreatedBy().getId(),
                Role.PRODUCER);
        String distributor = participantWallet(batch.getDistributorId(), Role.DISTRIBUTOR);
        String retailer = participantWallet(batch.getRetailerId(), Role.RETAILER);
        var assigned = List.of(producer, distributor, retailer);
        require(Set.copyOf(assigned).size() == assigned.size(), "Ba bên phải dùng ba địa chỉ ví khác nhau.");
        require(!assigned.contains(chain.getWalletAddress().toLowerCase(Locale.ROOT)),
                "Ví gửi giao dịch của backend phải khác ví của ba bên.");
        String registry = walletAddress(chain.getWalletContractAddress());
        BigInteger network = chain.chainId();

        String existing = chain.stageWallet(registry, network, batchId, RecordStage.PRODUCER);
        if (ZERO.equalsIgnoreCase(existing)) {
            var receipt = chain.bindBatchWallets(registry, network, batchId, producer, distributor, retailer);
            require(receipt.isStatusOK(), "Giao dịch gán ví thất bại.");
        } else {
            // Reconcile a mined transaction if the earlier request failed after submission.
            require(existing.equalsIgnoreCase(producer)
                            && chain.stageWallet(registry, network, batchId, RecordStage.DISTRIBUTOR)
                                    .equalsIgnoreCase(distributor)
                            && chain.stageWallet(registry, network, batchId, RecordStage.RETAILER)
                                    .equalsIgnoreCase(retailer),
                    "Ví đã gán trên chuỗi khác cấu hình hiện tại. Kiểm tra giao dịch gán ví.");
        }
        batch.setWalletRegistryAddress(registry);
        batch.setWalletRegistryChainId(network);
        return new BatchStatus("WALLET", registry, List.of());
    }

    @Transactional(readOnly = true)
    public SigningRequest signingRequest(Long batchId, CustomUserDetails actor) throws Exception {
        Batch batch = findBatch(batchId);
        RecordStage stage = actorStage(batch, actor);
        String root = eligibleRoot(batch, stage);
        chain.requireWalletChain(batch);
        String signer = assignedWallet(batch, stage);
        long deadline = Instant.now().getEpochSecond() + APPROVAL_SECONDS;
        var types = Map.of(
                "EIP712Domain", List.of(field("name", "string"), field("version", "string"),
                        field("chainId", "uint256"), field("verifyingContract", "address")),
                "StageApproval", List.of(field("batchId", "uint256"), field("stage", "uint8"),
                        field("root", "bytes32"), field("deadline", "uint256")));
        var domain = Map.of("name", "ChainTrack", "version", "2",
                "chainId", batch.getWalletRegistryChainId().toString(),
                "verifyingContract", batch.getWalletRegistryAddress());
        var message = Map.of("batchId", batch.getId().toString(), "stage", stage.ordinal(),
                "root", root, "deadline", deadline);
        return new SigningRequest(signer, batch.getWalletRegistryChainId().toString(), stage, deadline,
                Map.of("types", types, "primaryType", "StageApproval", "domain", domain, "message", message));
    }

    @Transactional(rollbackFor = Exception.class)
    public void approve(Long batchId, CustomUserDetails actor, SignedApproval request) throws Exception {
        Batch batch = locks.lock(batchId);
        RecordStage stage = actorStage(batch, actor);
        String root = eligibleRoot(batch, stage);
        long now = Instant.now().getEpochSecond();
        require(request.deadline() >= now && request.deadline() <= now + APPROVAL_SECONDS,
                "Chữ ký đã hết hạn hoặc có thời hạn không hợp lệ.");
        require(chain.checkApproval(batch, stage, root, request.deadline(),
                Numeric.hexStringToByteArray(request.signature())),
                "Sai ví được phân công hoặc root đã thay đổi. Hãy tải lại và ký lại.");
        StageWalletApproval approval = approvals.findByBatchIdAndStage(batchId, stage)
                .orElseGet(StageWalletApproval::new);
        approval.setBatchId(batchId);
        approval.setStage(stage);
        approval.setWalletAddress(assignedWallet(batch, stage));
        approval.setMerkleRoot(root);
        approval.setDeadline(request.deadline());
        approval.setSignature(request.signature());
        approvals.save(approval);
    }

    // Called under the existing batch write lock in AdminService.
    public AnchorResult submit(Batch batch, RecordStage stage, String root) throws Exception {
        if (batch.getWalletRegistryAddress() == null) {
            require(!canBind(batch), "Hãy gán ví ba bên trước khi anchor lô này.");
            return new AnchorResult(chain.anchorStageRoot(batch.getId(), stage, root), null);
        }
        String normalizedRoot = normalize(root);
        TransactionReceipt existing = chain.findWalletAnchorReceipt(batch, stage, normalizedRoot);
        if (existing != null) {
            return new AnchorResult(existing, assignedWallet(batch, stage));
        }
        var approval = approvals.findByBatchIdAndStage(batch.getId(), stage)
                .orElseThrow(() -> new BusinessException("Chưa có chữ ký của ví phụ trách giai đoạn " + stage + "."));
        require(approval.getMerkleRoot().equalsIgnoreCase(normalizedRoot),
                "Root đã thay đổi từ lúc ký. Bên phụ trách cần ký lại.");
        require(approval.getDeadline() >= Instant.now().getEpochSecond(), "Chữ ký hết hạn. Bên phụ trách cần ký lại.");
        var receipt = chain.anchorApprovedStage(batch, stage, normalizedRoot, approval.getDeadline(),
                Numeric.hexStringToByteArray(approval.getSignature()));
        return new AnchorResult(receipt, approval.getWalletAddress());
    }

    public record AnchorResult(TransactionReceipt receipt, String approvingWallet) {}

    private String assignedWallet(Batch batch, RecordStage stage) throws Exception {
        return chain.stageWallet(batch.getWalletRegistryAddress(), batch.getWalletRegistryChainId(), batch.getId(), stage);
    }

    private boolean canBind(Batch batch) {
        return chain.getWalletContractAddress() != null && !chain.getWalletContractAddress().isBlank()
                && anchors.findByBatch_IdOrderByRecordStageAsc(batch.getId()).isEmpty()
                && (batch.getChainTxHash() == null || batch.getChainTxHash().isBlank());
    }

    private String participantWallet(Long id, Role role) {
        require(id != null, "Lô chưa có bên phụ trách " + role + ".");
        User user = account(id);
        require(user.getRole() == role && Boolean.TRUE.equals(user.getEnabled()), role + " không hoạt động.");
        require(user.getWalletAddress() != null, role + " cần lưu địa chỉ ví trước.");
        return walletAddress(user.getWalletAddress());
    }

    private RecordStage actorStage(Batch batch, CustomUserDetails actor) {
        Long id = switch (actor.getRole()) {
            case PRODUCER -> batch.getCreatedBy() == null ? null : batch.getCreatedBy().getId();
            case DISTRIBUTOR -> batch.getDistributorId();
            case RETAILER -> batch.getRetailerId();
            default -> null;
        };
        if (!actor.getId().equals(id)) throw new AccessDeniedException("Tài khoản không được phân công cho lô này.");
        return RecordStage.valueOf(actor.getRole().name());
    }

    private String eligibleRoot(Batch batch, RecordStage stage) {
        require(batch.getWalletRegistryAddress() != null, "Lô chưa được Admin gán ví hoặc đang dùng registry cũ.");
        require(!anchors.existsByBatch_IdAndRecordStage(batch.getId(), stage), "Giai đoạn đã được anchor.");
        require(stage != RecordStage.RETAILER || batch.isRetailerRecordingClosed(), "Hãy chốt hồ sơ bán lẻ trước.");
        String root = rootOrNull(batch, stage);
        require(root != null, "Giai đoạn chưa có Merkle root để ký.");
        return root;
    }

    private static String rootOrNull(Batch batch, RecordStage stage) {
        String root = switch (stage) {
            case PRODUCER -> batch.getProducerMerkleRoot();
            case DISTRIBUTOR -> batch.getDistributorMerkleRoot();
            case RETAILER -> batch.getRetailerMerkleRoot();
        };
        return root == null || root.isBlank() ? null : normalize(root);
    }

    private static String normalize(String root) {
        return Numeric.prependHexPrefix(root).toLowerCase(Locale.ROOT);
    }

    private static String walletAddress(String value) {
        require(value != null && value.matches("0x[0-9a-fA-F]{40}") && !ZERO.equalsIgnoreCase(value),
                "Địa chỉ ví Ethereum không hợp lệ.");
        return value.toLowerCase(Locale.ROOT);
    }

    private static Map<String, String> field(String name, String type) {
        return Map.of("name", name, "type", type);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new BusinessException(message);
    }

    private Batch findBatch(Long batchId) {
        return batches.findById(batchId).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lô."));
    }

    private User account(Long id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản."));
    }
}
