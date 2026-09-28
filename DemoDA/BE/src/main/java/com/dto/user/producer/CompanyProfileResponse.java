package com.dto.user.producer;

import com.enums.Role;

public record CompanyProfileResponse(
        Long id,
        String companyName,
        String companyAddress,
        Role role
) {
}