package com.glowvera.domain;

import static com.glowvera.domain.OrderStatus.CANCELLED;
import static com.glowvera.domain.OrderStatus.DELIVERED;
import static com.glowvera.domain.OrderStatus.FAILED;
import static com.glowvera.domain.OrderStatus.PAID;
import static com.glowvera.domain.OrderStatus.PENDING_PAYMENT;
import static com.glowvera.domain.OrderStatus.PENDING_WHATSAPP;
import static com.glowvera.domain.OrderStatus.PROCESSING;
import static com.glowvera.domain.OrderStatus.SHIPPED;

import com.glowvera.common.AppException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;


public final class OrderStateMachine {

    private static final Map<OrderStatus, List<OrderStatus>> TRANSITIONS = new EnumMap<>(OrderStatus.class);

    static {
        TRANSITIONS.put(PENDING_PAYMENT, List.of(PAID, FAILED, CANCELLED));
        TRANSITIONS.put(PENDING_WHATSAPP, List.of(PAID, PROCESSING, CANCELLED));
        TRANSITIONS.put(PAID, List.of(PROCESSING, CANCELLED));
        TRANSITIONS.put(PROCESSING, List.of(SHIPPED, CANCELLED));
        TRANSITIONS.put(SHIPPED, List.of(DELIVERED));
        TRANSITIONS.put(DELIVERED, List.of());
        TRANSITIONS.put(CANCELLED, List.of());
        TRANSITIONS.put(FAILED, List.of());
    }

    private OrderStateMachine() {
    }

    public static List<OrderStatus> allowedNext(OrderStatus status) {
        return TRANSITIONS.getOrDefault(status, List.of());
    }

    public static boolean canTransition(OrderStatus from, OrderStatus to) {
        return allowedNext(from).contains(to);
    }

    public static void assertTransition(OrderStatus from, OrderStatus to) {
        if (!canTransition(from, to)) {
            throw AppException.conflict(
                    "An order cannot move from " + from + " to " + to,
                    Map.of("from", from, "to", to, "allowed", allowedNext(from)));
        }
    }

    public static StockEffect stockEffect(OrderStatus from, OrderStatus to) {
        if (from.isPending() && to.isCommitted()) {
            return StockEffect.COMMIT;
        }
        if (from.isPending() && (to == CANCELLED || to == FAILED)) {
            return StockEffect.RELEASE;
        }
        if (from.isCommitted() && to == CANCELLED) {
            return StockEffect.RESTOCK;
        }
        return StockEffect.NONE;
    }
}
