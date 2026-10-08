package com.glowvera.domain;

import static com.glowvera.domain.OrderStatus.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.glowvera.common.AppException;
import com.glowvera.common.ErrorCode;
import org.junit.jupiter.api.Test;

class OrderStateMachineTest {

    @Test
    void happyPathIsAllowed() {
        assertTrue(OrderStateMachine.canTransition(PENDING_PAYMENT, PAID));
        assertTrue(OrderStateMachine.canTransition(PAID, PROCESSING));
        assertTrue(OrderStateMachine.canTransition(PROCESSING, SHIPPED));
        assertTrue(OrderStateMachine.canTransition(SHIPPED, DELIVERED));
    }

    @Test
    void finalStatusesAreDeadEnds() {
        for (OrderStatus end : new OrderStatus[] {DELIVERED, CANCELLED, FAILED}) {
            assertTrue(OrderStateMachine.allowedNext(end).isEmpty(), end + " must have no next status");
        }
    }

    @Test
    void shippedOrdersCannotBeCancelled() {
        assertFalse(OrderStateMachine.canTransition(SHIPPED, CANCELLED));
    }

    @Test
    void illegalTransitionThrowsConflict() {
        AppException ex = assertThrows(AppException.class,
                () -> OrderStateMachine.assertTransition(DELIVERED, PAID));
        assertEquals(ErrorCode.CONFLICT, ex.getCode());
    }

    @Test
    void stockEffectsFollowTheLifecycle() {
        assertEquals(StockEffect.COMMIT, OrderStateMachine.stockEffect(PENDING_PAYMENT, PAID));
        assertEquals(StockEffect.COMMIT, OrderStateMachine.stockEffect(PENDING_WHATSAPP, PROCESSING));
        assertEquals(StockEffect.RELEASE, OrderStateMachine.stockEffect(PENDING_PAYMENT, CANCELLED));
        assertEquals(StockEffect.RELEASE, OrderStateMachine.stockEffect(PENDING_PAYMENT, FAILED));
        assertEquals(StockEffect.RESTOCK, OrderStateMachine.stockEffect(PAID, CANCELLED));
        assertEquals(StockEffect.RESTOCK, OrderStateMachine.stockEffect(PROCESSING, CANCELLED));
        assertEquals(StockEffect.NONE, OrderStateMachine.stockEffect(PAID, PROCESSING));
    }
}
