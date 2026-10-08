package com.glowvera.dto.response;

import com.glowvera.domain.StockMath.StockStatus;
import java.time.LocalDate;
import java.util.List;

public final class InventoryViews {

    private InventoryViews() {
    }

    public record StockAlert(
            Long variantId, String sku, String variantName, Long productId, String productName,
            int available, int lowStockThreshold, StockStatus stockStatus) {
    }

    public record BatchAlert(
            Long batchId, String batchCode, int quantity, LocalDate expiryDate, long daysToExpiry,
            Long variantId, String sku, String variantName, Long productId, String productName) {
    }

    public record Summary(int outOfStock, int lowStock, int nearExpiry, int expired) {
    }

    public record Alerts(
            int windowDays, Summary summary,
            List<StockAlert> outOfStock, List<StockAlert> lowStock,
            List<BatchAlert> nearExpiry, List<BatchAlert> expired) {
    }

    public record ReceiveBatchResult(AdminProductViews.BatchView batch, AdminProductViews.VariantView variant) {
    }

    public record WriteOffResult(int writtenOff, AdminProductViews.VariantView variant) {
    }
}
