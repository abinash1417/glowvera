package com.glowvera.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class StockMath {

    public static final int NEAR_EXPIRY_DAYS = 60;

    private StockMath() {
    }

    public enum StockStatus {
        OK,
        LOW_STOCK,
        OUT_OF_STOCK
    }

    public enum ExpiryStatus {
        OK,
        EMPTY,
        EXPIRED,
        NEAR_EXPIRY
    }

    public static int availableQty(int stockQty, int reservedQty) {
        return Math.max(stockQty - reservedQty, 0);
    }

    public static StockStatus stockStatus(int stockQty, int reservedQty, int lowStockThreshold) {
        int available = availableQty(stockQty, reservedQty);
        if (available == 0) {
            return StockStatus.OUT_OF_STOCK;
        }
        return available <= lowStockThreshold ? StockStatus.LOW_STOCK : StockStatus.OK;
    }

    public static long daysUntil(LocalDate expiryDate, LocalDate today) {
        return ChronoUnit.DAYS.between(today, expiryDate);
    }

    public static ExpiryStatus expiryStatus(int quantity, long daysToExpiry) {
        if (quantity == 0) {
            return ExpiryStatus.EMPTY;
        }
        if (daysToExpiry < 0) {
            return ExpiryStatus.EXPIRED;
        }
        return daysToExpiry <= NEAR_EXPIRY_DAYS ? ExpiryStatus.NEAR_EXPIRY : ExpiryStatus.OK;
    }
}
