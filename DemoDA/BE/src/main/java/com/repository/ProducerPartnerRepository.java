package com.repository;

import com.entity.ProducerPartner;
import com.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProducerPartnerRepository
        extends JpaRepository<ProducerPartner, Long> {

    Optional<ProducerPartner> findByProducerIdAndPartnerId(
            Long producerId,
            Long partnerId
    );

    boolean existsByProducerIdAndPartnerIdAndActiveTrueAndPartnerRoleAndPartnerEnabledTrue(
            Long producerId,
            Long partnerId,
            Role role
    );

    List<ProducerPartner>
    findByProducerIdAndPartnerRoleAndActiveTrueAndPartnerEnabledTrueOrderByPartnerCompanyNameAsc(
            Long producerId,
            Role role
    );
}