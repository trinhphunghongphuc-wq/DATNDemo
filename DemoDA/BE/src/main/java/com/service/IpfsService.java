package com.service;

import org.springframework.web.multipart.MultipartFile;

public interface IpfsService {

    String uploadJson(String rawJson);

    String getJson(String cid);

    String uploadImage(MultipartFile file);
}
