package com.service.impl;

import com.entity.Record;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.service.JsonExtractService;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class JsonExtractServiceImpl implements JsonExtractService {

    private final ObjectMapper objectMapper;

    public JsonExtractServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Map<String, String> extractBasicFields(Record record) {
        Map<String, String> result = new HashMap<>();

        if (record == null || record.getRawJson() == null || record.getRawJson().isBlank()) {
            return result;
        }

        try {
            Map<String, Object> rawData = objectMapper.readValue(
                    record.getRawJson(),
                    new TypeReference<Map<String, Object>>() {}
            );

            result.put("product", rawData.get("product") != null ? rawData.get("product").toString() : "");
            result.put("origin", rawData.get("origin") != null ? rawData.get("origin").toString() : "");
            result.put("weight", rawData.get("weight") != null ? rawData.get("weight").toString() : "");

        } catch (Exception ignored) {
        }

        return result;
    }
}