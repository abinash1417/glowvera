package com.glowvera.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "stock_batches",
        uniqueConstraints = @UniqueConstraint(columnNames = {"variant_id", "batch_code"}))
public class StockBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variant_id", nullable = false)
    private ProductVariant variant;

    @Column(name = "batch_code", nullable = false, length = 50)
    private String batchCode;

    /** Units remaining in this batch. */
    @Column(nullable = false)
    private int quantity;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @CreationTimestamp
    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    public StockBatch(ProductVariant variant, String batchCode, int quantity, LocalDate expiryDate) {
        this.variant = variant;
        this.batchCode = batchCode;
        this.quantity = quantity;
        this.expiryDate = expiryDate;
    }

    public boolean isExpiredOn(LocalDate today) {
        return expiryDate.isBefore(today);
    }

    public void take(int qty) {
        if (qty > quantity) {
            throw new IllegalStateException("Batch " + batchCode + " has only " + quantity + " units");
        }
        quantity -= qty;
    }

    public void putBack(int qty) {
        quantity += qty;
    }
}
