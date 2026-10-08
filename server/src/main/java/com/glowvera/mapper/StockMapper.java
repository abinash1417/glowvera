package com.glowvera.mapper;

import com.glowvera.domain.StockMath;
import com.glowvera.dto.response.AdminProductViews;
import com.glowvera.entity.ProductVariant;
import com.glowvera.entity.StockBatch;
import java.time.LocalDate;

public final class StockMapper {

    private StockMapper() {
    }

    public static AdminProductViews.VariantView toVariantView(ProductVariant v) {
        return new AdminProductViews.VariantView(
                v.getId(), v.getProduct().getId(), v.getSku(), v.getName(), v.getPriceCents(),
                v.getStockQty(), v.getReservedQty(), v.availableQty(), v.getLowStockThreshold(),
                v.isActive(), StockMath.stockStatus(v.getStockQty(), v.getReservedQty(), v.getLowStockThreshold()));
    }

    public static AdminProductViews.BatchView toBatchView(StockBatch b, LocalDate today) {
        long days = StockMath.daysUntil(b.getExpiryDate(), today);
        return new AdminProductViews.BatchView(
                b.getId(), b.getVariant().getId(), b.getBatchCode(), b.getQuantity(), b.getExpiryDate(),
                days, StockMath.expiryStatus(b.getQuantity(), days), b.getReceivedAt());
    }
}
