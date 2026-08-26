package com.controller;


import com.dto.product.ProductCategoryResponse;
import com.service.ProductCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product-categories")
@RequiredArgsConstructor
public class ProductCategoryController {

    private final ProductCategoryService productCategoryService;

    @GetMapping
    public List<ProductCategoryResponse> getCategories(
            @RequestParam(required = false) String keyword
    ) {
        return productCategoryService.searchCategories(keyword);
    }

    @GetMapping("/{id}")
    public ProductCategoryResponse getCategoryDetail(@PathVariable Long id) {
        return productCategoryService.getCategoryDetail(id);
    }
}