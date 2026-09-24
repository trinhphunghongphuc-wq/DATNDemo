package com.controller;

import com.dto.batch.AdminBatchListResponse;
import com.dto.batch.BatchDetailResponse;
import com.dto.record.AdminRecordDetailResponse;
import com.dto.record.RecentRecordResponse;
import com.dto.user.admin.AdminUserResponse;
import com.dto.user.admin.UpdateUserRoleRequest;
import com.dto.user.admin.UpdateUserStatusRequest;
import com.enums.RecordStage;
import com.security.CustomUserDetails;
import com.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.dto.blockchain.StageAnchorResponse;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/batches")
    public List<AdminBatchListResponse> getAllBatches() {
        return adminService.getAllBatches();
    }

    @GetMapping("/batches/filter")
    public List<AdminBatchListResponse> filterBatches(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status
    ) {
        return adminService.filterBatches(keyword, status);
    }
    @GetMapping("/batches/{batchId}")
    public BatchDetailResponse getBatchDetail(@PathVariable Long batchId) {
        return adminService.getBatchDetail(batchId);
    }

//    private Long getCurrentUserId(Authentication authentication) {
//        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
//        return userDetails.getId();
//    }

    @GetMapping("/records")
    public List<RecentRecordResponse> getAllRecords() {
        return adminService.getAllRecords();
    }

    @GetMapping("/users")
    public List<AdminUserResponse> getAllUsers() {
        return adminService.getAllUsers();
    }

    @GetMapping("/users/filter")
    public List<AdminUserResponse> filterUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role
    ) {
        return adminService.filterUsers(keyword, role);
    }

    @GetMapping("/users/{id}")
    public AdminUserResponse getUserById(@PathVariable Long id) {
        return adminService.getUserById(id);
    }

    @PutMapping("/users/{id}/role")
    public AdminUserResponse updateUserRole(
            @PathVariable Long id,
            @RequestBody UpdateUserRoleRequest request
    ) {
        return adminService.updateUserRole(id, request);
    }

    @PutMapping("/users/{id}/status")
    public AdminUserResponse updateUserStatus(
            @PathVariable Long id,
            @RequestBody UpdateUserStatusRequest request
    ) {
        return adminService.updateUserStatus(id, request);
    }

    @GetMapping("/records/{id}")
    public AdminRecordDetailResponse getRecordById(@PathVariable Long id) {
        return adminService.getRecordById(id);
    }

    @GetMapping("/records/filter")
    public List<AdminRecordDetailResponse> filterRecords(
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) String keyword
    ) {
        return adminService.filterRecords(batchId, keyword);
    }

    /*
     * CẢI TIẾN SO VỚI BASELINE YAO TRONG ĐỒ ÁN:
     * Admin anchor Merkle root riêng của từng giai đoạn,
     * thay vì một root chung cho toàn bộ batch.
     */
    @PostMapping("/batches/{id}/anchor-root/{stage}")
    public AdminBatchListResponse anchorBatchStageRoot(
            @PathVariable Long id,
            @PathVariable RecordStage stage
    ) {
        return adminService.anchorBatchStageRoot(
                id,
                stage
        );
    }

    @GetMapping("/batches/{id}/anchors")
    public List<StageAnchorResponse> getStageAnchors(
            @PathVariable Long id
    ) {
        return adminService.getStageAnchors(id);
    }
}