package com.service;

public interface SmartContractService {

    String getRoot(Long batchId) throws Exception;

    String setRoot(Long batchId, String merkleRoot) throws Exception;

}