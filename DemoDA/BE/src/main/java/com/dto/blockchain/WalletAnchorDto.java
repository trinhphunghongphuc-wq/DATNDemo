package com.dto.blockchain;

import com.enums.RecordStage;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.Map;

public final class WalletAnchorDto {
    private WalletAnchorDto() {}

    public record Address(@NotBlank @Pattern(regexp = "0x[0-9a-fA-F]{40}") String walletAddress) {}

    public record SignedApproval(@Positive long deadline,
                                 @NotBlank @Pattern(regexp = "0x[0-9a-fA-F]{130}") String signature) {}

    public record StageStatus(RecordStage stage, boolean ready, String signerAddress) {}
    public record BatchStatus(String mode, String registryAddress, List<StageStatus> approvals) {}
    public record SigningRequest(String walletAddress, String chainId, RecordStage stage, long deadline,
                                 Map<String, Object> typedData) {}
}
