package com.service;


import com.dto.verify.VerifyAllResponse;
import com.dto.verify.VerifyRequest;
import com.dto.verify.VerifyResponse;

public interface VerifyService {
    VerifyResponse verify(VerifyRequest request);

    VerifyAllResponse verifyAllRecords(Long batchId);
}