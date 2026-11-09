package com.security;

import com.entity.Record;
import com.repository.BatchRepository;
import com.repository.RecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("batchAccess")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BatchAccessService {

    private final BatchRepository batchRepository;
    private final RecordRepository recordRepository;

    public boolean canRead(
            Long batchId,
            Authentication authentication
    ) {
        CustomUserDetails user = currentUser(authentication);

        if (batchId == null || user == null) {
            return false;
        }

        return switch (user.getRole()) {
            case ADMIN -> true;

            case PRODUCER ->
                    batchRepository
                            .findByIdAndCreatedByUsername(
                                    batchId,
                                    user.getUsername()
                            )
                            .isPresent();

            case DISTRIBUTOR ->
                    batchRepository
                            .findByIdAndDistributorId(
                                    batchId,
                                    user.getId()
                            )
                            .isPresent();

            case RETAILER ->
                    batchRepository
                            .findByIdAndRetailerId(
                                    batchId,
                                    user.getId()
                            )
                            .isPresent();

            case CONSUMER -> false;
        };
    }

    public boolean canWrite(
            Long batchId,
            Authentication authentication
    ) {
        // Kiểm tra quyền trên lô.
        // Loại record và trạng thái vẫn do RecordService kiểm tra.
        return canRead(batchId, authentication);
    }

    public boolean canReadRecord(
            String recordKey,
            Authentication authentication
    ) {
        if (recordKey == null
                || recordKey.isBlank()
                || currentUser(authentication) == null) {
            return false;
        }

        Record record = recordRepository.findByRecordKey(recordKey);

        if (record == null || record.getBatch() == null) {
            return false;
        }

        // Xác định lô từ database, không lấy batchId do client khai báo.
        return canRead(
                record.getBatch().getId(),
                authentication
        );
    }

    private CustomUserDetails currentUser(
            Authentication authentication
    ) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal()
                instanceof CustomUserDetails user)
                || !user.isEnabled()) {
            return null;
        }

        return user;
    }
}