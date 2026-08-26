package com.service;

import com.dto.batch.AdminBatchListResponse;
import com.dto.batch.BatchDetailResponse;
import com.dto.record.AdminRecordDetailResponse;
import com.dto.record.RecentRecordResponse;
import com.dto.user.admin.AdminUserResponse;
import com.dto.user.admin.UpdateUserRoleRequest;
import com.dto.user.admin.UpdateUserStatusRequest;

import java.util.List;

public interface AdminService {

    List<AdminBatchListResponse> getAllBatches();

    List<AdminBatchListResponse> filterBatches(String keyword, String status);

    List<RecentRecordResponse> getAllRecords();

    List<AdminUserResponse> getAllUsers();

    List<AdminUserResponse> filterUsers(String keyword, String role);

    AdminUserResponse getUserById(Long id);

    AdminUserResponse updateUserRole(Long id, UpdateUserRoleRequest request);

    AdminUserResponse updateUserStatus(Long id, UpdateUserStatusRequest request);

    AdminRecordDetailResponse getRecordById(Long id);

    List<AdminRecordDetailResponse> filterRecords(Long batchId, String keyword);

    AdminBatchListResponse anchorBatchRoot(Long batchId) throws Exception;

    BatchDetailResponse getBatchDetail(Long batchId);
}