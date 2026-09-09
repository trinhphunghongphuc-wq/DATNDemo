package com.controller;

import com.dto.product.ProductDetailResponse;
import com.dto.product.ProductListResponse;
import com.dto.product.TraceabilityResponse;
import com.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/products")
    public List<ProductListResponse> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/products/{batchId}")
    public ProductDetailResponse getProductDetail(@PathVariable Long batchId) {
        return productService.getProductDetail(batchId);
    }

    @GetMapping("/traceability/{batchId}")
    public TraceabilityResponse getTraceability(@PathVariable Long batchId) {
        return productService.getTraceability(batchId);
    }

    // API public: QR của Producer chứa batchCode.
    @GetMapping("/traceability/batch/{batchCode}")
    public TraceabilityResponse getTraceabilityByBatchCode(
            @PathVariable String batchCode
    ) {
        return productService.getTraceabilityByBatchCode(batchCode);
    }

    @GetMapping("/products/filter")
    public List<ProductListResponse> searchAndFilterProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String status
    ) {
        return productService.searchAndFilterProducts(keyword, origin, status);
    }
}