package com.glowvera.service;

import com.glowvera.checkout.CheckoutStrategyRegistry;
import com.glowvera.checkout.ClientAction;
import com.glowvera.common.AppException;
import com.glowvera.domain.OrderSnapshot;
import com.glowvera.domain.OrderStatus;
import com.glowvera.domain.PaymentMethod;
import com.glowvera.security.AuthenticatedUser;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class WhatsAppService {

    private final OrderQueryService queries;
    private final CheckoutStrategyRegistry strategies;

    public WhatsAppService(OrderQueryService queries, CheckoutStrategyRegistry strategies) {
        this.queries = queries;
        this.strategies = strategies;
    }

    // The customer closed WhatsApp before sending: rebuild the link. //
    public Map<String, ClientAction> getLink(String code, AuthenticatedUser user) {
        OrderSnapshot order = queries.findOwnedSnapshot(code, user.id());
        if (order.paymentMethod() != PaymentMethod.WHATSAPP || order.status() != OrderStatus.PENDING_WHATSAPP) {
            throw AppException.conflict("This order is not waiting for WhatsApp confirmation");
        }
        return Map.of("clientAction", strategies.ready(PaymentMethod.WHATSAPP).buildClientAction(order));
    }
}
