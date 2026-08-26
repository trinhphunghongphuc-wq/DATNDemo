package com.dto.dashboard;

public class ProductsByOriginResponse {

    private String origin;
    private long count;

    public ProductsByOriginResponse() {
    }

    public ProductsByOriginResponse(String origin, long count) {
        this.origin = origin;
        this.count = count;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}