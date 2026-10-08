package com.glowvera.domain;

import com.glowvera.common.AppException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class CartRules {

    public static final int MAX_QTY_PER_LINE = 50;

    private CartRules() {
    }

    public record Line(long variantId, int quantity) {
    }

    public record PricedLine(long unitPriceCents, int quantity) {
    }

    public record Totals(long subtotalCents, long shippingCents, long totalCents) {
    }

    public enum LineStatus {
        OK,
        INSUFFICIENT_STOCK,
        UNAVAILABLE
    }

    public static List<Line> mergeItems(Collection<Line> items) {
        Map<Long, Integer> totals = new TreeMap<>();
        for (Line item : items) {
            totals.merge(item.variantId(), item.quantity(), Integer::sum);
        }
        return totals.entrySet().stream()
                .map(e -> {
                    if (e.getValue() > MAX_QTY_PER_LINE) {
                        throw AppException.badRequest(
                                "You can order at most " + MAX_QTY_PER_LINE + " units of one item");
                    }
                    return new Line(e.getKey(), e.getValue());
                })
                .toList();
    }

    public static Totals calculateTotals(Collection<PricedLine> lines, long shippingCents) {
        long subtotal = 0;
        for (PricedLine line : lines) {
            subtotal += line.unitPriceCents() * line.quantity();
        }
        return new Totals(subtotal, shippingCents, subtotal + shippingCents);
    }

    public static int availableForSale(int reservedQty, int sellableQty) {
        return Math.max(sellableQty - reservedQty, 0);
    }


    public static LineStatus evaluateLine(boolean purchasable, int reservedQty, int sellableQty, int quantity) {
        if (!purchasable) {
            return LineStatus.UNAVAILABLE;
        }
        return availableForSale(reservedQty, sellableQty) >= quantity
                ? LineStatus.OK
                : LineStatus.INSUFFICIENT_STOCK;
    }
}
