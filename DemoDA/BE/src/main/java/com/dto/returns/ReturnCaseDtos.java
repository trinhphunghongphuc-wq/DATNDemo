package com.dto.returns;

import com.entity.BatchReturnCase.Status;
import com.entity.BatchReturnEvent.Type;
import com.enums.Role;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class ReturnCaseDtos {
    private ReturnCaseDtos() {}

    public enum Outcome { TO_PRODUCER, DESTROY, REDISTRIBUTE }

    public record Approval(@NotNull @Positive BigDecimal weight, @Size(max = 2000) String note) {}
    public record Trip(@NotNull @Positive Long vehicleId,
                       @NotBlank @Size(max = 200) String warehouseName,
                       @NotBlank @Size(max = 300) String warehouseAddress,
                       @NotNull @Positive BigDecimal loadedWeight) {}
    public record Receipt(@NotNull @PositiveOrZero BigDecimal receivedWeight,
                          @NotBlank @Size(max = 2000) String note,
                          @NotBlank @Size(max = 120) String evidenceCid) {}
    public record Inspection(@NotBlank @Size(max = 150) String inspectorName,
                             @NotBlank @Size(max = 2000) String conclusion,
                             @NotBlank @Size(max = 120) String evidenceCid) {}
    public record Decision(@NotNull @PositiveOrZero BigDecimal toProducer,
                           @NotNull @PositiveOrZero BigDecimal destroy,
                           @NotNull @PositiveOrZero BigDecimal redistribute,
                           @NotBlank @Size(max = 2000) String reason) {
        public BigDecimal weight(Outcome outcome) {
            return switch (outcome) {
                case TO_PRODUCER -> toProducer;
                case DESTROY -> destroy;
                case REDISTRIBUTE -> redistribute;
            };
        }
    }
    public record Execution(@NotNull Outcome outcome,
                            @NotNull @Positive BigDecimal weight,
                            @NotBlank @Size(max = 2000) String note,
                            @NotBlank @Size(max = 120) String evidenceCid) {}
    public record SignedReading(@NotBlank @Size(max = 2048) String payload,
                                @NotBlank @Size(max = 128) String signature) {}
    public record Reading(@NotNull Long batchId, @NotNull Long tripId, @NotNull Long vehicleId,
                          @NotNull @Positive Long sequence, @NotNull Instant measuredAt,
                          @NotNull @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
                          @NotNull @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
                          @NotNull BigDecimal temperature,
                          @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal humidity) {}
    public record Summary(Long batchId, String batchCode, String name, Status status,
                          BigDecimal requestedWeight, BigDecimal receivedWeight, Instant updatedAt) {}
    public record Event(Long id, Role actorRole, Long actorId, Type type, Instant occurredAt, JsonNode details) {}
    public record Detail(Summary summary, Long tripId, Long vehicleId, String warehouseName, String warehouseAddress,
                         List<Event> events) {}
    public record Page<T>(List<T> items, boolean hasNext) {}
}
