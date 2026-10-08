package com.glowvera.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.glowvera.common.AppException;
import com.glowvera.domain.CartRules.Line;
import com.glowvera.domain.CartRules.LineStatus;
import com.glowvera.domain.CartRules.PricedLine;
import com.glowvera.domain.CartRules.Totals;
import java.util.List;
import org.junit.jupiter.api.Test;

class CartRulesTest {

    @Test
    void duplicateLinesAreMergedAndSortedById() {
        List<Line> merged = CartRules.mergeItems(List.of(new Line(9, 1), new Line(3, 2), new Line(9, 4)));
        assertEquals(List.of(new Line(3, 2), new Line(9, 5)), merged);
    }

    @Test
    void tooManyUnitsOfOneItemIsRejected() {
        assertThrows(AppException.class,
                () -> CartRules.mergeItems(List.of(new Line(1, 30), new Line(1, 30))));
    }

    @Test
    void totalsAreExactIntegersInCents() {
        Totals totals = CartRules.calculateTotals(
                List.of(new PricedLine(185000, 2), new PricedLine(145000, 1)), 35000);
        assertEquals(new Totals(515000, 35000, 550000), totals);
    }

    @Test
    void reservedUnitsReduceWhatCanBeSold() {
        assertEquals(7, CartRules.availableForSale(3, 10));
        assertEquals(0, CartRules.availableForSale(20, 10));
    }

    @Test
    void aLineIsOkInsufficientOrUnavailable() {
        assertEquals(LineStatus.OK, CartRules.evaluateLine(true, 0, 10, 5));
        assertEquals(LineStatus.INSUFFICIENT_STOCK, CartRules.evaluateLine(true, 8, 10, 5));
        assertEquals(LineStatus.UNAVAILABLE, CartRules.evaluateLine(false, 0, 10, 1));
    }

    @Test
    void orderCodesAreReadableAndZeroPadded() {
        assertEquals("GLW-2026-00042", OrderCodes.format(42, 2026));
        assertEquals(true, OrderCodes.isValid("GLW-2026-00042"));
        assertEquals(false, OrderCodes.isValid("hello"));
    }
}
