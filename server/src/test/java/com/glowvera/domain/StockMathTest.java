package com.glowvera.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.glowvera.domain.StockMath.ExpiryStatus;
import com.glowvera.domain.StockMath.StockStatus;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class StockMathTest {

    @Test
    void classifiesStock() {
        assertEquals(StockStatus.OUT_OF_STOCK, StockMath.stockStatus(5, 5, 5));
        assertEquals(StockStatus.LOW_STOCK, StockMath.stockStatus(8, 3, 5));
        assertEquals(StockStatus.OK, StockMath.stockStatus(40, 0, 5));
    }

    @Test
    void classifiesBatchExpiry() {
        assertEquals(ExpiryStatus.EMPTY, StockMath.expiryStatus(0, 10));
        assertEquals(ExpiryStatus.EXPIRED, StockMath.expiryStatus(5, -1));
        assertEquals(ExpiryStatus.NEAR_EXPIRY, StockMath.expiryStatus(5, 25));
        assertEquals(ExpiryStatus.OK, StockMath.expiryStatus(5, 300));
    }

    @Test
    void countsDaysUntilExpiry() {
        LocalDate today = LocalDate.of(2026, 10, 7);
        assertEquals(25, StockMath.daysUntil(today.plusDays(25), today));
        assertEquals(-3, StockMath.daysUntil(today.minusDays(3), today));
    }

    @Test
    void slugsAreUrlSafe() {
        assertEquals("vitamin-c-brightening-serum", Slugs.slugify("Vitamin C Brightening Serum"));
        assertEquals("creme-brulee", Slugs.slugify("  Crème Brûlée!! "));
        assertEquals("product", Slugs.slugify("!!!"));
    }
}
