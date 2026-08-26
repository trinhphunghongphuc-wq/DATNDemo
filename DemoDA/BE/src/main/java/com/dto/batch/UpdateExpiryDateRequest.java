package com.dto.batch;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateExpiryDateRequest {

    @NotNull
    private LocalDate expiryDate;
}