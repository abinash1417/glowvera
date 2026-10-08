package com.glowvera.dto.response;

import com.glowvera.domain.OrderStatus;
import com.glowvera.domain.PaymentMethod;
import com.glowvera.domain.PaymentStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class OrderViews {

    private OrderViews() {
    }

    //  customer
    public record CustomerItem(String productName, String variantName, long unitPriceCents,
                               int quantity, long lineTotalCents) {
    }

    public record CustomerOrder(
            String orderCode, OrderStatus status, PaymentMethod paymentMethod, String customerName,
            String district, long subtotalCents, long discountCents, long shippingCents, long totalCents,
            String currency, String couponCode, Instant reservationExpiresAt, Instant createdAt, List<CustomerItem> items) {
    }

    //  admin list
    public record AdminListItem(
            Long id, String orderCode, OrderStatus status, PaymentMethod paymentMethod, String customerName,
            String customerPhone, String district, long totalCents, int itemCount, Instant createdAt) {
    }

    //  admin detail
    public record CustomerInfo(Long userId, String name, String email, String phone,
                               String addressLine, String city, String district) {
    }

    public record BatchAllocation(String batchCode, int quantity, LocalDate expiryDate) {
    }

    public record AdminItem(
            Long id, Long variantId, String productName, String variantName, long unitPriceCents,
            int quantity, long lineTotalCents, List<BatchAllocation> batches) {
    }

    public record PaymentView(
            Long id, String provider, String payherePaymentId, PaymentStatus status, Integer statusCode,
            long amountCents, boolean signatureValid, Instant createdAt) {
    }

    public record HistoryView(OrderStatus fromStatus, OrderStatus toStatus, String changedBy,
                              String note, Instant createdAt) {
    }

    public record AdminDetail(
            Long id, String orderCode, OrderStatus status, List<OrderStatus> allowedNext,
            PaymentMethod paymentMethod, CustomerInfo customer, long subtotalCents, long discountCents,
            long shippingCents, long totalCents, String currency, String couponCode, String notes, Instant reservationExpiresAt,
            Instant createdAt, Instant updatedAt, List<AdminItem> items,
            List<PaymentView> payments, List<HistoryView> history) {
    }
}
