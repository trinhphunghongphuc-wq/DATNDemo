package com.controller;

import com.dto.user.producer.CompanyProfileRequest;
import com.dto.user.producer.CompanyProfileResponse;
import com.service.impl.CompanyProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/company-profile")
@PreAuthorize("hasAnyRole('DISTRIBUTOR', 'RETAILER')")
@RequiredArgsConstructor
public class CompanyProfileController {

    private final CompanyProfileService companyProfileService;

    @GetMapping
    public CompanyProfileResponse getMine(Authentication authentication) {
        return companyProfileService.getMyProfile(
                authentication.getName()
        );
    }

    @PutMapping
    public CompanyProfileResponse updateMine(
            @Valid @RequestBody CompanyProfileRequest request,
            Authentication authentication
    ) {
        return companyProfileService.updateMyProfile(
                authentication.getName(),
                request
        );
    }
}