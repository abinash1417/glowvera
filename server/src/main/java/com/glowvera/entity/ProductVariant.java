package com.glowvera.entity;

import com.glowvera.domain.StockMath;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "product_variants")
public class ProductVariant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "price_cents", nullable = false)
    private long priceCents;

    @Column(name = "stock_qty", nullable = false)
    private int stockQty;

    @Column(name = "reserved_qty", nullable = false)
    private int reservedQty;

    @Column(name = "low_stock_threshold", nullable = false)
    private int lowStockThreshold = 5;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "variant")
    private Set<StockBatch> batches = new HashSet<>();

    // ── behaviour ──

    public int availableQty() {
        return StockMath.availableQty(stockQty, reservedQty);
    }

    /** Unpaid order: hold units so nobody else can buy them. */
    public void reserve(int qty) {
        reservedQty += qty;
    }

    /** Unpaid order cancelled/failed/expired: give the held units back. */
    public void release(int qty) {
        if (qty > reservedQty) {
            throw new IllegalStateException("Cannot release " + qty + " units, only " + reservedQty + " reserved");
        }
        reservedQty -= qty;
    }

    /** Order paid: reserved units become permanently deducted. */
    public void commit(int qty) {
        if (qty > reservedQty || qty > stockQty) {
            throw new IllegalStateException("Cannot commit " + qty + " units for variant " + sku);
        }
        stockQty -= qty;
        reservedQty -= qty;
    }

    /** Paid order cancelled: units go back on the shelf. */
    public void restock(int qty) {
        stockQty += qty;
    }

    /** New batch received. */
    public void receive(int qty) {
        stockQty += qty;
    }

    /** Batch written off (expired / damaged). */
    public void writeOff(int qty) {
        if (stockQty - qty < reservedQty) {
            throw new IllegalStateException("Write-off would break reserved stock for variant " + sku);
        }
        stockQty -= qty;
    }
}
