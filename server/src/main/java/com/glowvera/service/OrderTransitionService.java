package com.glowvera.service;

import com.glowvera.common.AppException;
import com.glowvera.domain.OrderStateMachine;
import com.glowvera.domain.OrderStatus;
import com.glowvera.domain.PaymentMethod;
import com.glowvera.domain.StockEffect;
import com.glowvera.entity.CustomerOrder;
import com.glowvera.entity.OrderStatusHistory;
import com.glowvera.repository.OrderRepository;
import com.glowvera.repository.OrderStatusHistoryRepository;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;


@Service
public class OrderTransitionService {


    public record TransitionRequest(String actor, String note, boolean manual, Predicate<CustomerOrder> guard) {

        public static TransitionRequest system(String actor, String note) {
            return new TransitionRequest(actor, note, false, null);
        }

        public static TransitionRequest guarded(String actor, String note, Predicate<CustomerOrder> guard) {
            return new TransitionRequest(actor, note, false, guard);
        }

        public static TransitionRequest byAdmin(String adminEmail, String note) {
            return new TransitionRequest(adminEmail, note, true, null);
        }
    }

    private final OrderRepository orders;
    private final OrderStatusHistoryRepository history;
    private final StockService stock;

    public OrderTransitionService(OrderRepository orders, OrderStatusHistoryRepository history, StockService stock) {
        this.orders = orders;
        this.history = history;
        this.stock = stock;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public boolean transition(Long orderId, OrderStatus to, TransitionRequest request) {
        // Lock the order row: a webhook and an admin click on the same order now run one after the other.
        CustomerOrder order = orders.findByIdForUpdate(orderId)
                .orElseThrow(() -> AppException.notFound("Order not found"));

        if (request.guard() != null && !request.guard().test(order)) {
            return false;
        }

        if (request.manual()) {
            if (to == OrderStatus.FAILED) {
                throw AppException.forbidden("FAILED is set automatically by the payment system");
            }
            if (to == OrderStatus.PAID && order.getPaymentMethod() == PaymentMethod.PAYHERE) {
                throw AppException.forbidden("PayHere orders become PAID only after a verified payment");
            }
        }

        OrderStatus from = order.getStatus();
        OrderStateMachine.assertTransition(from, to);

        StockEffect effect = OrderStateMachine.stockEffect(from, to);
        switch (effect) {
            case COMMIT -> stock.commit(order);
            case RELEASE -> stock.release(order);
            case RESTOCK -> stock.restock(order);
            case NONE -> { }
        }

        order.setStatus(to);
        if (!to.isPending()) {
            order.setReservationExpiresAt(null);
        }
        history.save(new OrderStatusHistory(order, from, to, request.actor(), request.note()));
        return true;
    }
}
