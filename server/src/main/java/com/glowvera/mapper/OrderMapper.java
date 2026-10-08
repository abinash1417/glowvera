package com.glowvera.mapper;

import com.glowvera.domain.OrderSnapshot;
import com.glowvera.domain.OrderStateMachine;
import com.glowvera.dto.response.OrderViews;
import com.glowvera.entity.CustomerOrder;
import com.glowvera.entity.OrderItem;
import java.util.List;

public final class OrderMapper {

    private OrderMapper() {
    }

    public static OrderSnapshot toSnapshot(CustomerOrder o) {
        List<OrderSnapshot.Item> items = o.getItems().stream()
                .map(i -> new OrderSnapshot.Item(
                        i.getProductName(), i.getVariantName(), i.getUnitPriceCents(),
                        i.getQuantity(), i.getLineTotalCents()))
                .toList();
        return new OrderSnapshot(
                o.getOrderCode(), o.getStatus(), o.getPaymentMethod(), o.getCustomerName(),
                o.getCustomerEmail(), o.getCustomerPhone(), o.getAddressLine(), o.getCity(), o.getDistrict(),
                o.getSubtotalCents(), o.getDiscountCents(), o.getShippingCents(), o.getTotalCents(),
                o.getCurrency(), o.getCouponCode(),
                o.getReservationExpiresAt(), o.getNotes(), o.getCreatedAt(), items);
    }

    public static OrderViews.CustomerOrder toCustomerView(OrderSnapshot s) {
        return new OrderViews.CustomerOrder(
                s.orderCode(), s.status(), s.paymentMethod(), s.customerName(), s.district(),
                s.subtotalCents(), s.discountCents(), s.shippingCents(), s.totalCents(), s.currency(),
                s.couponCode(), s.reservationExpiresAt(), s.createdAt(),
                s.items().stream()
                        .map(i -> new OrderViews.CustomerItem(
                                i.productName(), i.variantName(), i.unitPriceCents(), i.quantity(), i.lineTotalCents()))
                        .toList());
    }

    public static OrderViews.AdminListItem toAdminListItem(CustomerOrder o) {
        int itemCount = o.getItems().stream().mapToInt(OrderItem::getQuantity).sum();
        return new OrderViews.AdminListItem(
                o.getId(), o.getOrderCode(), o.getStatus(), o.getPaymentMethod(), o.getCustomerName(),
                o.getCustomerPhone(), o.getDistrict(), o.getTotalCents(), itemCount, o.getCreatedAt());
    }

    public static OrderViews.AdminDetail toAdminDetail(CustomerOrder o) {
        OrderViews.CustomerInfo customer = new OrderViews.CustomerInfo(
                o.getUser() == null ? null : o.getUser().getId(),
                o.getCustomerName(), o.getCustomerEmail(), o.getCustomerPhone(),
                o.getAddressLine(), o.getCity(), o.getDistrict());

        List<OrderViews.AdminItem> items = o.getItems().stream()
                .map(i -> new OrderViews.AdminItem(
                        i.getId(), i.getVariant().getId(), i.getProductName(), i.getVariantName(),
                        i.getUnitPriceCents(), i.getQuantity(), i.getLineTotalCents(),
                        i.getAllocations().stream()
                                .map(a -> new OrderViews.BatchAllocation(
                                        a.getBatch().getBatchCode(), a.getQuantity(), a.getBatch().getExpiryDate()))
                                .toList()))
                .toList();

        List<OrderViews.PaymentView> payments = o.getPayments().stream()
                .map(p -> new OrderViews.PaymentView(
                        p.getId(), p.getProvider(), p.getPayherePaymentId(), p.getStatus(), p.getStatusCode(),
                        p.getAmountCents(), p.isSignatureValid(), p.getCreatedAt()))
                .toList();

        List<OrderViews.HistoryView> history = o.getHistory().stream()
                .map(h -> new OrderViews.HistoryView(
                        h.getFromStatus(), h.getToStatus(), h.getChangedBy(), h.getNote(), h.getCreatedAt()))
                .toList();

        return new OrderViews.AdminDetail(
                o.getId(), o.getOrderCode(), o.getStatus(), OrderStateMachine.allowedNext(o.getStatus()),
                o.getPaymentMethod(), customer, o.getSubtotalCents(), o.getDiscountCents(), o.getShippingCents(),
                o.getTotalCents(), o.getCurrency(), o.getCouponCode(), o.getNotes(), o.getReservationExpiresAt(), o.getCreatedAt(), o.getUpdatedAt(),
                items, payments, history);
    }
}
