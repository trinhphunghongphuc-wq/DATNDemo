package com.dto.verify;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifyRequest {

    @NotNull(message = "batchId must not be null")
    private Long batchId;

    @NotBlank(message = "recordKey must not be empty")
    private String recordKey;

    private String rawJson;
}