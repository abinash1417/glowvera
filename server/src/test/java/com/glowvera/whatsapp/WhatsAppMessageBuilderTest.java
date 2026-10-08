package com.glowvera.whatsapp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.glowvera.domain.OrderSnapshot;
import com.glowvera.domain.OrderStatus;
import com.glowvera.domain.PaymentMethod;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class WhatsAppMessageBuilderTest {

    private static OrderSnapshot order(List<OrderSnapshot.Item> items) {
        return new OrderSnapshot("GLW-2026-00007", OrderStatus.PENDING_WHATSAPP, PaymentMethod.WHATSAPP,
                "Nimal Perera", "nimal@example.com", "0771234567", "12 Galle Road", "Moratuwa", "Colombo",
                660000, 0, 35000, 695000, "LKR", null, null, "Please call before delivery", null, items);
    }

    private static final List<OrderSnapshot.Item> SAMPLE = List.of(
            new OrderSnapshot.Item("Hydra Glow Gel Moisturizer", "50ml", 185000, 2, 370000),
            new OrderSnapshot.Item("Hydra Glow Gel Moisturizer", "100ml", 320000, 1, 320000),
            new OrderSnapshot.Item("Vitamin C Brightening Serum", "15ml", 290000, 1, 290000));

    @Test
    void messageContainsCodeCustomerAddressAndTotal() {
        String message = WhatsAppMessageBuilder.buildOrderMessage(order(SAMPLE));
        assertTrue(message.contains("GLW-2026-00007"));
        assertTrue(message.contains("Nimal Perera"));
        assertTrue(message.contains("12 Galle Road, Moratuwa, Colombo"));
        assertTrue(message.contains("Total: Rs. 6,950.00"));
        assertTrue(message.contains("Please call before delivery"));
    }

    @Test
    void variantsOfOneProductShareASingleLine() {
        String message = WhatsAppMessageBuilder.buildOrderMessage(order(SAMPLE));
        assertTrue(message.contains("Hydra Glow Gel Moisturizer — 50ml × 2, 100ml × 1"));
        assertTrue(message.contains("Vitamin C Brightening Serum — 15ml × 1"));
    }

    @Test
    void veryLongCartFallsBackToCompactMessage() {
        List<OrderSnapshot.Item> items = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            items.add(new OrderSnapshot.Item(
                    "A product with quite a long descriptive name number " + i, "100ml", 75000, 2, 150000));
        }
        String message = WhatsAppMessageBuilder.buildOrderMessage(order(items));
        assertFalse(message.contains("(Rs."));
        assertTrue(message.contains("GLW-2026-00007"));
        assertTrue(message.contains("Total: Rs. 6,950.00"));
    }

    @Test
    void linkIsAWaMeUrlAndTheMessageSurvivesEncoding() {
        String message = WhatsAppMessageBuilder.buildOrderMessage(order(SAMPLE));
        String url = WhatsAppMessageBuilder.buildUrl("94771234567", message);
        assertTrue(url.startsWith("https://wa.me/94771234567?text="));
        assertFalse(url.contains("\n"));
        assertFalse(url.contains(" "));
        String decoded = URLDecoder.decode(url.substring(url.indexOf("?text=") + 6), StandardCharsets.UTF_8);
        assertEquals(message, decoded);
    }
}
