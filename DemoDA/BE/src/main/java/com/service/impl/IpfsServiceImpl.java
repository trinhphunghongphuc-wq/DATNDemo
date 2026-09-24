package com.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.service.IpfsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;

@Service
public class IpfsServiceImpl implements IpfsService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public IpfsServiceImpl(RestClient.Builder restClientBuilder,
                           ObjectMapper objectMapper,
                           @Value("${ipfs.api.url:http://127.0.0.1:5001}") String ipfsApiUrl) {
        this.restClient = restClientBuilder.baseUrl(ipfsApiUrl).build();
        this.objectMapper = objectMapper;
    }

    @Override
    public String uploadJson(String rawJson) {
        validateJson(rawJson);

        ByteArrayResource jsonFile = new ByteArrayResource(
                rawJson.getBytes(StandardCharsets.UTF_8)) {
            @Override
            public String getFilename() {
                return "record.json";
            }
        };

        MultiValueMap<String, Object> multipartBody = new LinkedMultiValueMap<>();
        multipartBody.add("file", jsonFile);

        try {
            String responseBody = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v0/add")
                            .queryParam("pin", "true")
                            .queryParam("cid-version", "1")
                            .build())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(multipartBody)
                    .retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                throw new IllegalStateException("IPFS returned an empty response");
            }

            JsonNode responseJson = objectMapper.readTree(responseBody.trim());
            String cid = responseJson.path("Hash").asText();
            if (cid.isBlank()) {
                throw new IllegalStateException("IPFS response does not contain a CID");
            }
            return cid;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot upload JSON to IPFS", e);
        }
    }

    @Override
    public String getJson(String cid) {
        if (cid == null || cid.isBlank()) {
            throw new IllegalArgumentException("CID cannot be blank");
        }

        try {
            String json = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v0/cat")
                            .queryParam("arg", cid.trim())
                            .build())
                    .retrieve()
                    .body(String.class);

            if (json == null || json.isBlank()) {
                throw new IllegalStateException("IPFS returned empty content for CID: " + cid);
            }
            return json;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot read JSON from IPFS for CID: " + cid, e);
        }
    }

    private void validateJson(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            throw new IllegalArgumentException("rawJson cannot be blank");
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(rawJson);
            if (jsonNode == null || !jsonNode.isObject()) {
                throw new IllegalArgumentException("rawJson must be a JSON object");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid rawJson format", e);
        }
    }
}
