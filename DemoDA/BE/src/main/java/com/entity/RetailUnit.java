package com.entity;

import com.enums.RetailUnitStatus;
import com.enums.RetailUnitType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "retail_units")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetailUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "retail_code", unique = true, nullable = false)
    private String retailCode;

    @Column(nullable = false)
    private String productName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RetailUnitType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RetailUnitStatus status;

    @Column(name = "allocated_weight", nullable = false)
    private Double allocatedWeight;

    // PACKAGED
    private Double packageWeight;
    private Integer packageQuantity;
    private Double pricePerPackage;

    // BULK
    private Double pricePerKg;
    private LocalDate expiryDate;

    private String qrContent;

    @Column(name = "retailer_id", nullable = false)
    private Long retailerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    private LocalDateTime createdAt;
}