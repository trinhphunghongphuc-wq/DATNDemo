package com.service;



import com.dto.product.ProductCategoryResponse;

import java.util.List;

public interface ProductCategoryService {

    List<ProductCategoryResponse> getActiveCategories();

    List<ProductCategoryResponse> searchCategories(String keyword);

    ProductCategoryResponse getCategoryDetail(Long id);
}