package com.dto.user.producer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyProfileRequest(
        @NotBlank @Size(max = 200) String companyName,
        @NotBlank @Size(max = 500) String companyAddress
) {
}