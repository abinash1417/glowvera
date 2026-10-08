package com.glowvera.checkout;

import com.glowvera.common.AppException;
import com.glowvera.common.ErrorCode;
import com.glowvera.config.AppProperties;
import com.glowvera.domain.OrderSnapshot;
import com.glowvera.domain.OrderStatus;
import com.glowvera.domain.PaymentMethod;
import com.glowvera.whatsapp.WhatsAppMessageBuilder;
import java.time.Duration;
import org.springframework.stereotype.Component;

@Component
public class WhatsAppCheckoutStrategy implements CheckoutStrategy {

    private final AppProperties props;

    public WhatsAppCheckoutStrategy(AppProperties props) {
        this.props = props;
    }

    @Override
    public PaymentMethod method() {
        return PaymentMethod.WHATSAPP;
    }

    @Override
    public OrderStatus initialStatus() {
        return OrderStatus.PENDING_WHATSAPP;
    }

    // The shop confirms by chat, so the customer gets a full day. //
    @Override
    public Duration reservationWindow() {
        return Duration.ofHours(24);
    }

    @Override
    public void assertReady() {
        if (!props.whatsappConfigured()) {
            throw new AppException(ErrorCode.WHATSAPP_UNAVAILABLE,
                    "WhatsApp ordering is temporarily unavailable. Please pay online instead.");
        }
    }

    @Override
    public ClientAction buildClientAction(OrderSnapshot order) {
        String message = WhatsAppMessageBuilder.buildOrderMessage(order);
        return ClientAction.WhatsAppLink.of(
                WhatsAppMessageBuilder.buildUrl(props.whatsappNumber(), message), message);
    }
}
