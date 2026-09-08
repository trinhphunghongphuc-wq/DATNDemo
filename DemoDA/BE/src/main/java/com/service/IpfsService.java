package com.service;

public interface IpfsService {

    String uploadJson(String rawJson);

    String getJson(String cid);
}
