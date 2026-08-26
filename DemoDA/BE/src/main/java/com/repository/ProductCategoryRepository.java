package com.repository;

import com.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {

    List<ProductCategory> findByActiveTrue();

    List<ProductCategory> findByNameContainingIgnoreCaseAndActiveTrue(String keyword);
}