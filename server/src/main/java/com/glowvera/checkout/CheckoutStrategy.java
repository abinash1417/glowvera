package com.glowvera.checkout;

import com.glowvera.domain.OrderSnapshot;
import com.glowvera.domain.OrderStatus;
import com.glowvera.domain.PaymentMethod;
import java.time.Duration;


public interface CheckoutStrategy {

    PaymentMethod method();

    OrderStatus initialStatus();

    Duration reservationWindow();

    void assertReady();

    ClientAction buildClientAction(OrderSnapshot order);
}
