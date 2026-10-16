package com.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.service.IpfsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class IpfsServiceImpl implements IpfsService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public IpfsServiceImpl(
            RestClient.Builder restClientBuilder,
            ObjectMapper objectMapper,
            @Value("${ipfs.api.url:http://127.0.0.1:5001}") String ipfsApiUrl
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(60_000);

        this.restClient = restClientBuilder
                .requestFactory(requestFactory)
                .baseUrl(ipfsApiUrl)
                .build();
        this.objectMapper = objectMapper;
    }

    @Override
    public String uploadJson(String rawJson) {
        validateJson(rawJson);

        return uploadBytes(
                rawJson.getBytes(StandardCharsets.UTF_8),
                "record.json"
        );
    }

    @Override
    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Image file cannot be empty");
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            filename = "evidence-image";
        }

        try {
            return uploadBytes(file.getBytes(), filename);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Cannot read image file: " + e.getMessage(),
                    e
            );
        }
    }

    private String uploadBytes(byte[] content, String filename) {
        ByteArrayResource resource = new ByteArrayResource(content) {
            @Override
            public String getFilename() {
                return filename;
            }
        };

        MultiValueMap<String, Object> multipartBody =
                new LinkedMultiValueMap<>();
        multipartBody.add("file", resource);

        try {
            String responseBody = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v0/add")
                            .queryParam("pin", "true")
                            .queryParam("cid-version", "1")
                            .build())
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(multipartBody)
                    .exchange((request, response) -> {
                        String body = new String(
                                response.getBody().readAllBytes(),
                                StandardCharsets.UTF_8
                        );

                        if (!response.getStatusCode().is2xxSuccessful()) {
                            throw new IllegalStateException(
                                    "IPFS HTTP "
                                            + response.getStatusCode()
                                            + ": "
                                            + body
                            );
                        }

                        return body;
                    });

            if (responseBody == null || responseBody.isBlank()) {
                throw new IllegalStateException(
                        "IPFS returned an empty response"
                );
            }

            JsonNode responseJson = objectMapper.readTree(
                    responseBody.trim()
            );

            String cid = responseJson.path("Hash").asText();

            if (cid.isBlank()) {
                throw new IllegalStateException(
                        "IPFS response does not contain a CID: "
                                + responseBody
                );
            }

            return cid;
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Cannot upload file to IPFS ("
                            + e.getClass().getSimpleName()
                            + "): "
                            + e.getMessage(),
                    e
            );
        }
    }

    @Override
    public String getJson(String cid) {
        if (cid == null || cid.isBlank()) {
            throw new IllegalArgumentException("CID cannot be blank");
        }

        try {
            byte[] content = restClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v0/cat")
                            .queryParam("arg", cid.trim())
                            .build())
                    .retrieve()
                    .body(byte[].class);

            if (content == null || content.length == 0) {
                throw new IllegalStateException(
                        "IPFS returned empty content for CID: " + cid
                );
            }

            return new String(content, StandardCharsets.UTF_8);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Cannot read JSON from IPFS for CID: "
                            + cid
                            + ". Cause: "
                            + e.getMessage(),
                    e
            );
        }
    }

    private void validateJson(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            throw new IllegalArgumentException("rawJson cannot be blank");
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(rawJson);

            if (jsonNode == null || !jsonNode.isObject()) {
                throw new IllegalArgumentException(
                        "rawJson must be a JSON object"
                );
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Invalid rawJson format",
                    e
            );
        }
    }
}