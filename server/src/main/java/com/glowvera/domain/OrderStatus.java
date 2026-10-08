package com.glowvera.domain;

public enum OrderStatus {
    PENDING_PAYMENT,   // PayHere order created, waiting for payment
    PENDING_WHATSAPP,  // WhatsApp order saved, waiting for admin confirmation
    PAID,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    FAILED;

    // Stock is only RESERVED while an order is pending. //
    public boolean isPending() {
        return this == PENDING_PAYMENT || this == PENDING_WHATSAPP;
    }

    // Stock is DEDUCTED once an order is committed. //
    public boolean isCommitted() {
        return this == PAID || this == PROCESSING || this == SHIPPED || this == DELIVERED;
    }
}
