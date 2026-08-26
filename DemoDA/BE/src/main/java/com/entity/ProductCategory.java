package com.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private Double temperatureMin;

    private Double temperatureMax;

    private Double humidityMin;

    private Double humidityMax;

    private String recommendedPackaging;

    private String description;

    private Boolean driedProduct;

    private Boolean active;
}