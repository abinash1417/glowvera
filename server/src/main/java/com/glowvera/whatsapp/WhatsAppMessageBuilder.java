package com.glowvera.whatsapp;

import com.glowvera.domain.Money;
import com.glowvera.domain.OrderSnapshot;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public final class WhatsAppMessageBuilder {

    static final int COMPACT_THRESHOLD = 1500;

    private WhatsAppMessageBuilder() {
    }

    public static String buildOrderMessage(OrderSnapshot order) {
        String full = render(order, true);
        return full.length() <= COMPACT_THRESHOLD ? full : render(order, false);
    }

    public static String buildUrl(String number, String message) {
        return "https://wa.me/" + number + "?text=" + encodeUriComponent(message);
    }

    private static String render(OrderSnapshot order, boolean withPrices) {
        List<String> lines = new ArrayList<>();
        lines.add("*New order " + order.orderCode() + "*");
        lines.add("");
        lines.add("*Customer*");
        lines.add(order.customerName());
        lines.add(order.customerPhone());
        lines.add(order.addressLine() + ", " + order.city() + ", " + order.district());
        lines.add("");
        lines.add("*Items*");
        lines.addAll(itemLines(order.items(), withPrices));
        lines.add("");
        lines.add("Subtotal: " + Money.formatRupees(order.subtotalCents()));
        if (order.discountCents() > 0) {
            lines.add("Discount (" + order.couponCode() + "): -" + Money.formatRupees(order.discountCents()));
        }
        lines.add("Delivery (" + order.district() + "): " + Money.formatRupees(order.shippingCents()));
        lines.add("*Total: " + Money.formatRupees(order.totalCents()) + "*");

        if (order.notes() != null && !order.notes().isBlank()) {
            lines.add("");
            lines.add("Note: " + order.notes());
        }
        lines.add("");
        lines.add("Please confirm my order and share the payment details. Thank you!");

        return String.join("\n", lines);
    }

    private static List<String> itemLines(List<OrderSnapshot.Item> items, boolean withPrices) {
        // LinkedHashMap keeps products in the order they were first added to the cart
        Map<String, ProductGroup> groups = new LinkedHashMap<>();
        for (OrderSnapshot.Item item : items) {
            ProductGroup group = groups.computeIfAbsent(item.productName(), k -> new ProductGroup());
            group.parts.add(item.variantName() + " × " + item.quantity());
            group.totalCents += item.lineTotalCents();
        }

        List<String> result = new ArrayList<>();
        for (Map.Entry<String, ProductGroup> entry : groups.entrySet()) {
            ProductGroup group = entry.getValue();
            String line = "• " + entry.getKey() + " — " + String.join(", ", group.parts);
            result.add(withPrices ? line + " (" + Money.formatRupees(group.totalCents) + ")" : line);
        }
        return result;
    }

    private static final class ProductGroup {
        final List<String> parts = new ArrayList<>();
        long totalCents;
    }

    static String encodeUriComponent(String text) {
        return URLEncoder.encode(text, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("%21", "!")
                .replace("%27", "'")
                .replace("%28", "(")
                .replace("%29", ")")
                .replace("%7E", "~");
    }
}
