package com.glowvera.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.glowvera.domain.StockMath.ExpiryStatus;
import com.glowvera.domain.StockMath.StockStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class AdminProductViews {

    private AdminProductViews() {
    }

    public record VariantView(
            Long id, Long productId, String sku, String name, long priceCents,
            int stockQty, int reservedQty, int available, int lowStockThreshold,
            @JsonProperty("isActive") boolean isActive, StockStatus stockStatus) {
    }

    public record BatchView(
            Long id, Long variantId, String batchCode, int quantity, LocalDate expiryDate,
            long daysToExpiry, ExpiryStatus expiryStatus, Instant receivedAt) {
    }

    public record VariantDetail(
            Long id, Long productId, String sku, String name, long priceCents,
            int stockQty, int reservedQty, int available, int lowStockThreshold,
            @JsonProperty("isActive") boolean isActive, StockStatus stockStatus,
            List<BatchView> batches) {
    }

    public record ListItem(
            Long id, String name, String slug, String brand, String imageUrl,
            @JsonProperty("isActive") boolean isActive, Refs.CategoryRef category,
            Instant createdAt, List<VariantView> variants, int totalAvailable) {
    }

    public record Detail(
            Long id, String name, String slug, String brand, String description,
            String ingredients, String howToUse, String imageUrl,
            @JsonProperty("isActive") boolean isActive, Refs.CategoryRef category,
            List<Refs.NameRef> skinTypes, List<Refs.NameRef> concerns,
            Instant createdAt, Instant updatedAt, List<VariantDetail> variants) {
    }
}
