package com.service.impl;

import com.dto.batch.BatchRequest;
import com.dto.record.RecordRequest;
import com.dto.returns.ReturnCaseDtos.*;
import com.entity.Batch;
import com.entity.BatchReturnCase;
import com.entity.BatchReturnCase.Status;
import com.entity.BatchReturnEvent;
import com.entity.BatchReturnEvent.Type;
import com.entity.Vehicle;
import com.enums.BatchStatus;
import com.enums.RecordType;
import com.enums.Role;
import com.exception.BusinessException;
import com.exception.ResourceNotFoundException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.repository.BatchRepository;
import com.repository.BatchReturnCaseRepository;
import com.repository.BatchReturnEventRepository;
import com.repository.VehicleRepository;
import com.security.CustomUserDetails;
import com.service.BatchService;
import com.service.DeviceSignatureService;
import com.service.IpfsService;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class BatchReturnService {
    private final BatchReturnCaseRepository cases;
    private final BatchReturnEventRepository events;
    private final BatchRepository batches;
    private final VehicleRepository vehicles;
    private final BatchLockService locks;
    private final NotificationService notifications;
    private final IpfsService ipfs;
    private final ObjectMapper json;
    private final Validator validator;
    private final DeviceSignatureService signatures;
    private final DemoVehicleRunRegistry demoRuns;
    private final BatchService batchService;
    private final ProducerPartnerService partners;

    @Transactional(propagation = Propagation.MANDATORY)
    public void open(Batch batch, Long retailerId, double weight, String reason, String evidenceCid, String recordKey) {
        require(batch.getCreatedBy() != null && batch.getDistributorId() != null,
                "Lô cần có Producer sở hữu và Distributor được phân công.");
        BigDecimal requested = BigDecimal.valueOf(weight);
        require(requested.signum() > 0, "Khối lượng hoàn trả phải lớn hơn 0.");
        require(reason.trim().length() <= 2000, "Lý do trả hàng tối đa 2000 ký tự.");
        BatchReturnCase item = cases.saveAndFlush(new BatchReturnCase(batch.getId(), requested, Instant.now()));
        append(item, retailerId, Role.RETAILER, Type.REJECTED, Map.of(
                "weight", requested, "reason", reason.trim(), "evidenceCid", cid(evidenceCid),
                "recordKey", recordKey));
        notifyParty(batch, Role.PRODUCER, "REQUESTED", "Lô bị từ chối", "Xem bằng chứng và duyệt chuyến trả hàng.");
    }

    @Transactional(readOnly = true)
    public Page<Summary> list(CustomUserDetails user, int page) {
        var items = cases.findForParty(user.getId(), user.getRole().name(), page(page));
        return new Page<>(items.map(item -> summary(item, batch(item.getBatchId()))).getContent(), items.hasNext());
    }

    @Transactional(readOnly = true)
    public Detail get(Long id, CustomUserDetails user) {
        Batch batch = batch(id);
        authorize(batch, user, user.getRole());
        return detail(findCase(id), batch);
    }

    @Transactional(readOnly = true)
    public Page<Event> telemetry(Long id, CustomUserDetails user, int page) {
        authorize(batch(id), user, user.getRole());
        findCase(id);
        var points = events.findByBatchIdAndEventTypeOrderByIdDesc(id, Type.TELEMETRY, page(page));
        return new Page<>(points.map(this::event).getContent(), points.hasNext());
    }

    public Detail approve(Long id, CustomUserDetails user, Approval request) {
        Batch batch = locks.lock(id);
        authorize(batch, user, Role.PRODUCER);
        BatchReturnCase item = at(id, Status.REQUESTED);
        sameWeight(request.weight(), item.getRequestedWeight());
        transition(item, Status.APPROVED, user, Type.APPROVED, request);
        notifyParty(batch, Role.DISTRIBUTOR, "APPROVED", "Đã duyệt hoàn trả", "Chọn xe và kho để tạo chuyến về.");
        notifyParty(batch, Role.RETAILER, "APPROVED", "Đã duyệt hoàn trả", "Distributor sẽ bố trí nhận lại toàn bộ lô.");
        return detail(item, batch);
    }

    public Detail startTrip(Long id, CustomUserDetails user, Trip request) {
        Batch batch = locks.lock(id);
        authorize(batch, user, Role.DISTRIBUTOR);
        BatchReturnCase item = at(id, Status.APPROVED);
        sameWeight(request.loadedWeight(), item.getRequestedWeight());
        Vehicle vehicle = activeVehicle(request.vehicleId(), user.getId());
        demoRuns.checkTransportAllowed(vehicle.getId());
        require(vehicle.getDevicePublicKey() != null && !vehicle.getDevicePublicKey().isBlank(),
                "Xe cần có public key Ed25519 đã đăng ký.");
        require(!cases.existsByVehicleIdAndStatus(vehicle.getId(), Status.IN_TRANSIT),
                "Xe đang thực hiện chuyến hoàn trả khác.");
        item.setVehicleId(vehicle.getId());
        item.setWarehouseName(request.warehouseName().trim());
        item.setWarehouseAddress(request.warehouseAddress().trim());
        transition(item, Status.IN_TRANSIT, user, Type.TRIP_STARTED, request);
        return detail(item, batch);
    }

    public Event addReading(Long id, CustomUserDetails user, SignedReading request) {
        authorize(locks.lock(id), user, Role.DISTRIBUTOR);
        BatchReturnCase item = at(id, Status.IN_TRANSIT);
        BatchReturnEvent trip = step(id, Type.TRIP_STARTED);
        Vehicle vehicle = activeVehicle(item.getVehicleId(), user.getId());
        demoRuns.checkTransportAllowed(vehicle.getId());
        Reading reading;
        try {
            reading = json.readerFor(Reading.class)
                    .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .readValue(request.payload());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Payload IoT không hợp lệ.", e);
        }
        require(reading != null && validator.validate(reading).isEmpty(), "Thiếu hoặc sai thông số IoT.");
        require(id.equals(reading.batchId()) && trip.getId().equals(reading.tripId())
                && vehicle.getId().equals(reading.vehicleId()), "Bản tin không thuộc chuyến hoặc xe đã chọn.");
        require(!reading.measuredAt().isBefore(trip.getOccurredAt())
                && !reading.measuredAt().isAfter(Instant.now().plusSeconds(120)),
                "Thời điểm đo không nằm trong khoảng thời gian của chuyến.");
        String publicKey = vehicle.getDevicePublicKey();
        require(publicKey != null && !publicKey.isBlank()
                && signatures.verifySignature(request.payload(), request.signature(), publicKey),
                "Chữ ký bản tin hoàn trả không hợp lệ.");
        long lastSequence = vehicle.getLastSequence() == null ? 0L : vehicle.getLastSequence();
        require(lastSequence < Long.MAX_VALUE && reading.sequence() == lastSequence + 1,
                "Sequence không hợp lệ. Hãy tải lại sequence của xe.");
        vehicle.setLastSequence(reading.sequence());
        return event(append(item, user.getId(), user.getRole(), Type.TELEMETRY, Map.of(
                "reading", reading, "signedPayload", request.payload(), "signature", request.signature(),
                "publicKeyAtSigning", publicKey)));
    }

    public Detail receive(Long id, CustomUserDetails user, Receipt request) {
        Batch batch = locks.lock(id);
        authorize(batch, user, Role.DISTRIBUTOR);
        BatchReturnCase item = at(id, Status.IN_TRANSIT);
        cid(request.evidenceCid());
        require(request.receivedWeight().compareTo(item.getRequestedWeight()) <= 0,
                "Khối lượng thực nhận vượt khối lượng đã giao lên xe.");
        item.setReceivedWeight(request.receivedWeight());
        batch.setRemainingWeight(request.receivedWeight().doubleValue());
        batch.setStatus(BatchStatus.RETURNED_TO_DISTRIBUTOR);
        transition(item, Status.QUARANTINED, user, Type.RECEIVED, request);
        notifyParty(batch, Role.PRODUCER, "RECEIVED", "Lô trả đã nhập kho", "Kiểm chứng lô tại " + item.getWarehouseName() + ".");
        return detail(item, batch);
    }

    public Detail inspect(Long id, CustomUserDetails user, Inspection request) {
        Batch batch = locks.lock(id);
        authorize(batch, user, Role.PRODUCER);
        BatchReturnCase item = at(id, Status.QUARANTINED);
        cid(request.evidenceCid());
        transition(item, Status.INSPECTED, user, Type.INSPECTED, request);
        return detail(item, batch);
    }

    public Detail decide(Long id, CustomUserDetails user, Decision request) {
        Batch batch = locks.lock(id);
        authorize(batch, user, Role.PRODUCER);
        BatchReturnCase item = at(id, Status.INSPECTED);
        sameWeight(request.toProducer().add(request.destroy()).add(request.redistribute()), item.getReceivedWeight());
        if (request.redistribute().signum() > 0) validateRedistribution(batch);
        transition(item, Status.DECIDED, user, Type.DECIDED, request);
        notifyParty(batch, Role.DISTRIBUTOR, "DECIDED", "Đã có phương án xử lý lô", "Xem khối lượng và ghi bằng chứng thực hiện.");
        return detail(item, batch);
    }

    public Detail execute(Long id, CustomUserDetails user, Execution request) {
        Batch batch = locks.lock(id);
        Role actor = request.outcome() == Outcome.TO_PRODUCER ? Role.PRODUCER : Role.DISTRIBUTOR;
        authorize(batch, user, actor);
        BatchReturnCase item = at(id, Status.DECIDED);
        String evidenceCid = cid(request.evidenceCid());
        Decision decision = json.convertValue(read(step(id, Type.DECIDED).getDetails()), Decision.class);
        sameWeight(request.weight(), decision.weight(request.outcome()));
        Set<Outcome> completed = executed(id);
        require(!completed.contains(request.outcome()), "Phương án đã được xác nhận thực hiện.");
        var details = new java.util.LinkedHashMap<String, Object>();
        details.put("outcome", request.outcome());
        details.put("weight", request.weight());
        details.put("note", request.note().trim());
        details.put("evidenceCid", evidenceCid);
        if (request.outcome() == Outcome.REDISTRIBUTE) {
            details.put("childBatchId", redispatch(batch, request.weight()).getId());
        }
        BigDecimal remaining = item.getReceivedWeight().subtract(request.weight());
        for (Outcome outcome : completed) {
            remaining = remaining.subtract(decision.weight(outcome));
        }
        batch.setRemainingWeight(remaining.doubleValue());
        append(item, user.getId(), user.getRole(), Type.EXECUTED, details);
        if (actor == Role.DISTRIBUTOR) {
            notifyParty(batch, Role.PRODUCER, "EXECUTED_" + request.outcome().name(), "Đã ghi nhận xử lý hàng trả", "Kiểm tra bằng chứng thực hiện trong hồ sơ.");
        }
        return detail(item, batch);
    }

    public Detail close(Long id, CustomUserDetails user) {
        Batch batch = locks.lock(id);
        authorize(batch, user, Role.PRODUCER);
        BatchReturnCase item = at(id, Status.DECIDED);
        Decision decision = json.convertValue(read(step(id, Type.DECIDED).getDetails()), Decision.class);
        Set<Outcome> completed = executed(id);
        for (Outcome outcome : Outcome.values()) {
            require(decision.weight(outcome).signum() == 0 || completed.contains(outcome),
                    "Còn phương án chưa có xác nhận thực hiện.");
        }
        transition(item, Status.CLOSED, user, Type.CLOSED, Map.of("receivedWeight", item.getReceivedWeight()));
        notifyParty(batch, Role.DISTRIBUTOR, "CLOSED", "Hồ sơ hoàn trả đã đóng", "Producer đã đối soát các phương án.");
        notifyParty(batch, Role.RETAILER, "CLOSED", "Hồ sơ hoàn trả đã đóng", "Xem kết quả xử lý lô bị từ chối.");
        return detail(item, batch);
    }

    @Transactional(readOnly = true)
    public String uploadEvidence(Long id, CustomUserDetails user, MultipartFile file) {
        Batch batch = batch(id);
        authorize(batch, user, user.getRole());
        BatchReturnCase item = findCase(id);
        require(item.getStatus() != Status.CLOSED && user.getRole() != Role.RETAILER,
                "Ảnh mới được thêm bởi bên đang xử lý hồ sơ.");
        require(file.getSize() <= 5 * 1024 * 1024
                && List.of("image/jpeg", "image/png", "image/webp").contains(
                        Objects.toString(file.getContentType(), "")),
                "Chọn ảnh JPG, PNG hoặc WebP tối đa 5 MB.");
        return cid(ipfs.uploadImage(file));
    }

    private Batch redispatch(Batch parent, BigDecimal weight) {
        validateRedistribution(parent);
        double childWeight = weight.doubleValue();
        require(Double.isFinite(childWeight) && childWeight > 0
                && BigDecimal.valueOf(childWeight).compareTo(weight) == 0,
                "Khối lượng cấp cho lô con không biểu diễn được chính xác.");
        BatchRequest next = new BatchRequest();
        next.setName(parent.getName());
        next.setDistributorId(parent.getDistributorId());
        next.setRetailerId(parent.getRetailerId());
        next.setProductCategoryId(parent.getProductCategory() == null ? null : parent.getProductCategory().getId());
        next.setExpiryDate(parent.getExpiryDate());
        next.setTotalWeight(childWeight);
        RecordRequest provenance = new RecordRequest();
        provenance.setRecordType(RecordType.PRODUCTION);
        provenance.setRawJson(write(Map.of("eventType", "RETURN_REDISTRIBUTION",
                "sourceBatchId", parent.getId(), "sourceBatchCode", parent.getBatchCode(), "weight", weight)));
        next.setRecords(List.of(provenance));
        Batch child = batchService.createBatchEntity(next, Role.PRODUCER);
        child.setCreatedBy(parent.getCreatedBy());
        return batches.save(child);
    }

    private void validateRedistribution(Batch batch) {
        require(batch.getExpiryDate() == null || !batch.getExpiryDate().isBefore(LocalDate.now()),
                "Lô đã hết hạn, không thể phân phối lại.");
        partners.validateAssignment(batch.getCreatedBy(), batch.getDistributorId(), batch.getRetailerId());
    }

    private void authorize(Batch batch, CustomUserDetails user, Role role) {
        Long owner = switch (role) {
            case PRODUCER -> batch.getCreatedBy() == null ? null : batch.getCreatedBy().getId();
            case DISTRIBUTOR -> batch.getDistributorId();
            case RETAILER -> batch.getRetailerId();
            default -> null;
        };
        if (owner == null || user.getRole() != role || !owner.equals(user.getId())) {
            throw new AccessDeniedException("Bạn không có quyền thao tác trên hồ sơ hoàn trả này.");
        }
    }

    private void notifyParty(Batch batch, Role role, String action, String title, String message) {
        Long recipient = switch (role) {
            case PRODUCER -> batch.getCreatedBy().getId();
            case RETAILER -> batch.getRetailerId();
            case DISTRIBUTOR -> batch.getDistributorId();
            default -> throw new IllegalArgumentException("Vai trò không tham gia hồ sơ.");
        };
        notifications.batchReturnUpdated(recipient, role, batch, action, title, message);
    }

    private Batch batch(Long id) {
        return batches.findById(id).orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lô hàng."));
    }

    private BatchReturnCase findCase(Long id) {
        return cases.findById(id).orElseThrow(() -> new ResourceNotFoundException("Lô chưa có hồ sơ hoàn trả."));
    }

    private BatchReturnCase at(Long id, Status status) {
        BatchReturnCase item = findCase(id);
        require(item.getStatus() == status, "Trạng thái hồ sơ đã thay đổi hoặc chưa cho phép thao tác.");
        return item;
    }

    private Vehicle activeVehicle(Long vehicleId, Long distributorId) {
        return vehicles.findActiveVehicleForUpdate(vehicleId, distributorId)
                .orElseThrow(() -> new BusinessException("Xe không hoạt động hoặc không thuộc nhà phân phối."));
    }

    private BatchReturnEvent step(Long id, Type type) {
        return events.findFirstByBatchIdAndEventType(id, type).orElseThrow();
    }

    private Set<Outcome> executed(Long id) {
        return events.findByBatchIdAndEventTypeOrderById(id, Type.EXECUTED).stream()
                .map(event -> Outcome.valueOf(read(event.getDetails()).path("outcome").asText()))
                .collect(java.util.stream.Collectors.toSet());
    }

    private void transition(BatchReturnCase item, Status status, CustomUserDetails user, Type type, Object data) {
        item.setStatus(status);
        append(item, user.getId(), user.getRole(), type, data);
    }

    private BatchReturnEvent append(BatchReturnCase item, Long userId, Role role, Type type, Object data) {
        Instant now = Instant.now();
        if (type != Type.TELEMETRY) item.setUpdatedAt(now);
        return events.save(new BatchReturnEvent(item.getBatchId(), userId, role, type, now, write(data)));
    }

    private Detail detail(BatchReturnCase item, Batch batch) {
        var entries = events.findByBatchIdAndEventTypeNotOrderById(item.getBatchId(), Type.TELEMETRY);
        Long tripId = entries.stream().filter(entry -> entry.getEventType() == Type.TRIP_STARTED)
                .map(BatchReturnEvent::getId).findFirst().orElse(null);
        return new Detail(summary(item, batch), tripId, item.getVehicleId(), item.getWarehouseName(),
                item.getWarehouseAddress(), entries.stream().map(this::event).toList());
    }

    private Summary summary(BatchReturnCase item, Batch batch) {
        return new Summary(batch.getId(), batch.getBatchCode(), batch.getName(), item.getStatus(),
                item.getRequestedWeight(), item.getReceivedWeight(), item.getUpdatedAt());
    }

    private Event event(BatchReturnEvent entry) {
        return new Event(entry.getId(), entry.getActorRole(), entry.getActorId(), entry.getEventType(),
                entry.getOccurredAt(), read(entry.getDetails()));
    }

    private String write(Object data) {
        try {
            return json.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không ghi được dữ liệu hoàn trả.", e);
        }
    }

    private JsonNode read(String data) {
        try {
            return json.readTree(data);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Dữ liệu hoàn trả không hợp lệ.", e);
        }
    }

    private static String cid(String value) {
        if (value == null || !value.trim().matches("(?:Qm[1-9A-HJ-NP-Za-km-z]{44}|b[a-z2-7]{20,119})")) {
            throw new IllegalArgumentException("CID ảnh không hợp lệ. Hãy tải ảnh lên IPFS.");
        }
        return value.trim();
    }

    private static PageRequest page(int page) {
        require(page >= 0, "Trang phải từ 0 trở lên.");
        return PageRequest.of(page, 25);
    }

    private static void sameWeight(BigDecimal actual, BigDecimal expected) {
        require(actual.compareTo(expected) == 0, "Khối lượng phải khớp với " + expected.toPlainString() + " kg.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new BusinessException(message);
    }
}
