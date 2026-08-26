package com.service;

import com.entity.Record;

import java.util.Map;

public interface JsonExtractService {

    Map<String, String> extractBasicFields(Record record);
}