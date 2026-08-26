package com.dto.user.producer;

import com.dto.batch.BatchRequest;
import com.dto.record.ProducerStorageRequirementRequest;
import com.dto.record.RecordRequest;
import com.enums.RecordType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ProducerBatchRequest extends BatchRequest {
    private ProducerStorageRequirementRequest storageRequirement;

    @Override
    @NotNull(message = "Expiry date is required")
    public LocalDate getExpiryDate() {
        return super.getExpiryDate();
    }


}