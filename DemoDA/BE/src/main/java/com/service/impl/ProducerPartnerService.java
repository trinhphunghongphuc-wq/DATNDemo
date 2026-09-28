package com.service.impl;

import com.dto.user.producer.CompanyProfileResponse;
import com.entity.ProducerPartner;
import com.entity.User;
import com.enums.Role;
import com.repository.ProducerPartnerRepository;
import com.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProducerPartnerService {

    private final ProducerPartnerRepository partnerRepository;
    private final UserRepository userRepository;

    // Các tài khoản có thể được Producer chọn làm đối tác.
    @Transactional(readOnly = true)
    public List<CompanyProfileResponse> directory(Role role) {
        requirePartnerRole(role);

        return userRepository
                .findByRoleAndEnabledTrueAndCompanyNameIsNotNullAndCompanyAddressIsNotNull(role)
                .stream()
                .filter(this::hasCompanyProfile)
                .map(this::toResponse)
                .toList();
    }

    // Danh sách đối tác đang hoạt động của Producer hiện tại.
    @Transactional(readOnly = true)
    public List<CompanyProfileResponse> myPartners(
            String producerUsername,
            Role role
    ) {
        requirePartnerRole(role);
        User producer = getProducer(producerUsername);

        return partnerRepository
                .findByProducerIdAndPartnerRoleAndActiveTrueAndPartnerEnabledTrueOrderByPartnerCompanyNameAsc(
                        producer.getId(),
                        role
                )
                .stream()
                .map(ProducerPartner::getPartner)
                .filter(this::hasCompanyProfile)
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CompanyProfileResponse addPartner(
            String producerUsername,
            Long partnerId
    ) {
        User producer = getProducer(producerUsername);

        User partner = userRepository.findById(partnerId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Partner account not found: " + partnerId
                        )
                );

        requirePartnerRole(partner.getRole());

        if (!Boolean.TRUE.equals(partner.getEnabled())
                || !hasCompanyProfile(partner)) {
            throw new IllegalArgumentException(
                    "Partner must be active and have company name and address"
            );
        }

        ProducerPartner relation = partnerRepository
                .findByProducerIdAndPartnerId(
                        producer.getId(),
                        partner.getId()
                )
                .orElseGet(() -> {
                    ProducerPartner newRelation = new ProducerPartner();
                    newRelation.setProducer(producer);
                    newRelation.setPartner(partner);
                    return newRelation;
                });

        // Nếu trước đó đã bỏ đối tác, thêm lại bằng cách kích hoạt quan hệ cũ.
        relation.setActive(true);
        partnerRepository.save(relation);

        return toResponse(partner);
    }

    @Transactional
    public void removePartner(
            String producerUsername,
            Long partnerId
    ) {
        User producer = getProducer(producerUsername);

        ProducerPartner relation = partnerRepository
                .findByProducerIdAndPartnerId(
                        producer.getId(),
                        partnerId
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Partner not found: " + partnerId
                        )
                );

        relation.setActive(false);
    }

    // Gọi trước khi lưu batch mới.
    @Transactional(readOnly = true)
    public void validateAssignment(
            User producer,
            Long distributorId,
            Long retailerId
    ) {
        if (distributorId == null && retailerId == null) {
            return;
        }

        if (distributorId == null || retailerId == null) {
            throw new IllegalArgumentException(
                    "Both distributorId and retailerId are required"
            );
        }

        validateSelectedPartner(
                producer,
                distributorId,
                Role.DISTRIBUTOR
        );

        validateSelectedPartner(
                producer,
                retailerId,
                Role.RETAILER
        );
    }

    private void validateSelectedPartner(
            User producer,
            Long partnerId,
            Role expectedRole
    ) {
        User partner = userRepository.findById(partnerId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Partner account not found: " + partnerId
                        )
                );

        boolean activeRelation = partnerRepository
                .existsByProducerIdAndPartnerIdAndActiveTrueAndPartnerRoleAndPartnerEnabledTrue(
                        producer.getId(),
                        partnerId,
                        expectedRole
                );

        if (partner.getRole() != expectedRole
                || !Boolean.TRUE.equals(partner.getEnabled())
                || !hasCompanyProfile(partner)
                || !activeRelation) {
            throw new IllegalArgumentException(
                    expectedRole
                            + " is not an active partner of this Producer: "
                            + partnerId
            );
        }
    }

    private User getProducer(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Producer account not found"
                        )
                );

        if (user.getRole() != Role.PRODUCER) {
            throw new IllegalArgumentException(
                    "Producer account required"
            );
        }

        return user;
    }

    private void requirePartnerRole(Role role) {
        if (role != Role.DISTRIBUTOR && role != Role.RETAILER) {
            throw new IllegalArgumentException(
                    "Role must be DISTRIBUTOR or RETAILER"
            );
        }
    }

    private boolean hasCompanyProfile(User user) {
        return user.getCompanyName() != null
                && !user.getCompanyName().isBlank()
                && user.getCompanyAddress() != null
                && !user.getCompanyAddress().isBlank();
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