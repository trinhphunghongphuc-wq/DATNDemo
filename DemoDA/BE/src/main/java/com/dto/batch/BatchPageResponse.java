package com.dto.batch;

import java.util.List;
public record BatchPageResponse(List<BatchSearchItem> content, int number, int size,
        long totalElements, int totalPages) {}
