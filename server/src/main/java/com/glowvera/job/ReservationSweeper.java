package com.glowvera.job;

import com.glowvera.service.OrderExpiryService;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


@Component
public class ReservationSweeper {

    private static final Logger log = LoggerFactory.getLogger(ReservationSweeper.class);

    private final OrderExpiryService expiry;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public ReservationSweeper(OrderExpiryService expiry) {
        this.expiry = expiry;
    }

    @Scheduled(initialDelay = 10_000, fixedDelay = 60_000)
    public void sweep() {
        if (!running.compareAndSet(false, true)) {
            return; // never overlap two runs
        }
        try {
            int count = expiry.expireStaleReservations();
            if (count > 0) {
                log.info("Released {} expired reservation(s)", count);
            }
        } catch (RuntimeException e) {
            log.error("Reservation sweeper failed", e);
        } finally {
            running.set(false);
        }
    }
}
