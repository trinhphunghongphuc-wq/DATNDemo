package com.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@EnabledIfEnvironmentVariable(named = "RUN_IPFS_TEST", matches = "(?i)true")
class IpfsServiceIntegrationTest {

    private ObjectMapper objectMapper;
    private IpfsServiceImpl ipfsService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        ipfsService = new IpfsServiceImpl(
                RestClient.builder(),
                objectMapper,
                "http://127.0.0.1:5001"
        );
    }

    @Test
    void uploadAndReadJson_shouldReturnOriginalJson() throws Exception {
        String originalJson = """
                {
                  "recordType": "HARVEST",
                  "farm": "Da Lat Farm",
                  "quantity": 100,
                  "unit": "kg"
                }
                """;

        String cid = ipfsService.uploadJson(originalJson);
        String downloadedJson = ipfsService.getJson(cid);

        assertFalse(cid.isBlank());

        JsonNode originalNode = objectMapper.readTree(originalJson);
        JsonNode downloadedNode = objectMapper.readTree(downloadedJson);

        assertEquals(originalNode, downloadedNode);

        System.out.println("IPFS CID: " + cid);
    }
}