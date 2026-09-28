package com.service.impl;

import com.dto.user.producer.CompanyProfileRequest;
import com.dto.user.producer.CompanyProfileResponse;
import com.entity.User;
import com.enums.Role;
import com.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CompanyProfileService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public CompanyProfileResponse getMyProfile(String username) {
        User user = getPartnerAccount(username);
        return toResponse(user);
    }

    @Transactional
    public CompanyProfileResponse updateMyProfile(
            String username,
            CompanyProfileRequest request
    ) {
        User user = getPartnerAccount(username);

        user.setCompanyName(request.companyName().trim());
        user.setCompanyAddress(request.companyAddress().trim());

        return toResponse(user);
    }

    private User getPartnerAccount(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new IllegalArgumentException("Account not found")
                );

        if (user.getRole() != Role.DISTRIBUTOR
                && user.getRole() != Role.RETAILER) {
            throw new IllegalArgumentException(
                    "Only Distributor and Retailer have partner profiles"
            );
        }

        return user;
    }

    private CompanyProfileResponse toResponse(User user) {
        return new CompanyProfileResponse(
                user.getId(),
                user.getCompanyName(),
                user.getCompanyAddress(),
                user.getRole()
        );
    }
}