package com.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "retail_unit_price_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetailUnitPriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "retail_unit_id", nullable = false)
    private RetailUnit retailUnit;

    @Column(name = "retailer_id", nullable = false)
    private Long retailerId;

    @Column(name = "old_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal oldPrice;

    @Column(name = "new_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal newPrice;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;
}