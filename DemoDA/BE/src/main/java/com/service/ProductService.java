package com.service;

import com.dto.product.ProductDetailResponse;
import com.dto.product.ProductListResponse;
import com.dto.product.TraceabilityResponse;

import java.util.List;

public interface ProductService {

    List<ProductListResponse> getAllProducts();

    ProductDetailResponse getProductDetail(Long batchId);

    TraceabilityResponse getTraceability(Long batchId);

    List<ProductListResponse> searchAndFilterProducts(String keyword, String origin, String status);
}