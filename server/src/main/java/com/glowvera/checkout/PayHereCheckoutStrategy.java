package com.glowvera.checkout;

import com.glowvera.common.AppException;
import com.glowvera.common.ErrorCode;
import com.glowvera.config.AppProperties;
import com.glowvera.domain.OrderSnapshot;
import com.glowvera.domain.OrderStatus;
import com.glowvera.domain.PaymentMethod;
import com.glowvera.payment.PayHereFormBuilder;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class PayHereCheckoutStrategy implements CheckoutStrategy {

    private final AppProperties props;
    private final PayHereFormBuilder formBuilder;

    public PayHereCheckoutStrategy(AppProperties props, PayHereFormBuilder formBuilder) {
        this.props = props;
        this.formBuilder = formBuilder;
    }

    @Override
    public PaymentMethod method() {
        return PaymentMethod.PAYHERE;
    }

    @Override
    public OrderStatus initialStatus() {
        return OrderStatus.PENDING_PAYMENT;
    }

    @Override
    public Duration reservationWindow() {
        return Duration.ofMinutes(30);
    }

    @Override
    public void assertReady() {
        if (!props.payhere().configured()) {
            throw new AppException(ErrorCode.PAYMENT_UNAVAILABLE,
                    "Online payment is temporarily unavailable. Please order via WhatsApp instead.");
        }
    }

    @Override
    public ClientAction buildClientAction(OrderSnapshot order) {
        return formBuilder.build(order);
    }
}
