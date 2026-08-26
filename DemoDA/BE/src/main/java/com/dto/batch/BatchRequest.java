package com.dto.batch;



import com.dto.record.ProducerStorageRequirementRequest;
import com.dto.record.RecordRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
@Getter
@Setter
@Data
@RequiredArgsConstructor
public class BatchRequest {
    private ProducerStorageRequirementRequest storageRequirement;
    @NotBlank
    private String name;

    @NotEmpty
    private List<RecordRequest> records;

    private LocalDate expiryDate;

    //nghiep vu cua role distributor
    private Long distributorId;
    private Long retailerId;



    private Long productCategoryId;
    private Double totalWeight;
}