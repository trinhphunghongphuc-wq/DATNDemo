package com.service.impl;

import com.dto.batch.BatchPageResponse;
import com.dto.batch.BatchSearchItem;
import com.entity.Batch;
import com.enums.BatchStatus;
import com.repository.BatchRepository;
import com.repository.RecordRepository;
import com.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashMap;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class BatchSearchService {
    private final BatchRepository batches;
    private final RecordRepository records;

    @Transactional(readOnly = true)
    public BatchPageResponse search(CustomUserDetails user, String keyword, String status, int page, int size) {
        if (user == null || !user.isEnabled()) {
            throw new AccessDeniedException("Bạn không có quyền xem lô hàng.");
        }
        if (page < 0 || size < 1 || size > 50) {
            throw new IllegalArgumentException("page >= 0; size từ 1 đến 50");
        }
        String text = keyword == null ? "" : keyword.trim();
        if (text.length() > 200) throw new IllegalArgumentException("Từ khóa tối đa 200 ký tự.");

        // Scope in SQL, before filtering/counting/pagination. Never trust a client userId.
        Specification<Batch> scope = switch (user.getRole()) {
            case PRODUCER -> (root, query, cb) -> cb.equal(root.get("createdBy").get("id"), user.getId());
            case DISTRIBUTOR -> (root, query, cb) -> cb.equal(root.get("distributorId"), user.getId());
            case RETAILER -> (root, query, cb) -> cb.equal(root.get("retailerId"), user.getId());
            default -> throw new AccessDeniedException("Vai trò không hỗ trợ danh sách này.");
        };

        if (!text.isEmpty()) {
            // Search literal %, _ and ! rather than treating input as SQL wildcards.
            String pattern = "%" + text.toLowerCase(Locale.ROOT)
                    .replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
            scope = scope.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), pattern, '!'),
                    cb.like(cb.lower(root.get("batchCode")), pattern, '!')));
        }

        if (status != null && !status.isBlank()) {
            BatchStatus requiredStatus;
            try {
                requiredStatus = BatchStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException error) {
                throw new IllegalArgumentException("Trạng thái lô hàng không hợp lệ.");
            }
            scope = scope.and((root, query, cb) -> cb.equal(root.get("status"), requiredStatus));
        }

        var result = batches.findAll(scope,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt", "id")));
        var counts = new HashMap<Long, Long>();
        if (result.hasContent()) {
            records.countRecordsForBatches(result.getContent().stream().map(Batch::getId).toList())
                    .forEach(count -> counts.put(count.getBatchId(), count.getRecordCount()));
        }
        return new BatchPageResponse(result.getContent().stream()
                .map(batch -> BatchSearchItem.from(batch, counts.getOrDefault(batch.getId(), 0L))).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }
}
