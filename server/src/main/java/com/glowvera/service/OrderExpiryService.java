package com.glowvera.service;

import com.glowvera.domain.OrderStatus;
import com.glowvera.repository.OrderRepository;
import com.glowvera.service.OrderTransitionService.TransitionRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
public class OrderExpiryService {

    private static final Logger log = LoggerFactory.getLogger(OrderExpiryService.class);
    private static final int BATCH_SIZE = 50;

    private final OrderRepository orders;
    private final OrderTransitionService transitions;
    private final Clock clock;

    public OrderExpiryService(OrderRepository orders, OrderTransitionService transitions, Clock clock) {
        this.orders = orders;
        this.transitions = transitions;
        this.clock = clock;
    }

    public int expireStaleReservations() {
        List<Long> stale = orders.findExpiredPendingIds(
                EnumSet.of(OrderStatus.PENDING_PAYMENT, OrderStatus.PENDING_WHATSAPP),
                Instant.now(clock), PageRequest.of(0, BATCH_SIZE));

        int expired = 0;
        for (Long id : stale) {
            try {
                // The guard re-checks INSIDE the lock, so an order paid a millisecond ago is never cancelled by mistake.
                boolean done = transitions.transition(id, OrderStatus.CANCELLED, TransitionRequest.guarded(
                        "system", "Reservation expired",
                        o -> o.getStatus().isPending()
                                && o.getReservationExpiresAt() != null
                                && o.getReservationExpiresAt().isBefore(Instant.now(clock))));
                if (done) {
                    expired++;
                }
            } catch (RuntimeException e) {
                log.error("Could not expire order {}", id, e);
            }
        }
        return expired;
    }
}
