package com.service.impl;


import com.dto.product.ProductCategoryResponse;
import com.entity.ProductCategory;
import com.repository.ProductCategoryRepository;
import com.service.ProductCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductCategoryServiceImpl implements ProductCategoryService {

    private final ProductCategoryRepository productCategoryRepository;

    @Override
    public List<ProductCategoryResponse> getActiveCategories() {
        return productCategoryRepository.findByActiveTrue()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<ProductCategoryResponse> searchCategories(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getActiveCategories();
        }

        return productCategoryRepository
                .findByNameContainingIgnoreCaseAndActiveTrue(keyword)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public ProductCategoryResponse getCategoryDetail(Long id) {
        ProductCategory category = productCategoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Product category not found"));

        return toResponse(category);
    }

    private ProductCategoryResponse toResponse(ProductCategory category) {
        return ProductCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .temperatureMin(category.getTemperatureMin())
                .temperatureMax(category.getTemperatureMax())
                .humidityMin(category.getHumidityMin())
                .humidityMax(category.getHumidityMax())
                .recommendedPackaging(category.getRecommendedPackaging())
                .description(category.getDescription())
                .driedProduct(category.getDriedProduct())
                .build();
    }
}