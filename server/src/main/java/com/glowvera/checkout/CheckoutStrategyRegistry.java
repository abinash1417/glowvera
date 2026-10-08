package com.glowvera.checkout;

import com.glowvera.common.AppException;
import com.glowvera.domain.PaymentMethod;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CheckoutStrategyRegistry {

    private final Map<PaymentMethod, CheckoutStrategy> strategies = new EnumMap<>(PaymentMethod.class);

    public CheckoutStrategyRegistry(List<CheckoutStrategy> all) {
        all.forEach(s -> strategies.put(s.method(), s));
    }

    public CheckoutStrategy ready(PaymentMethod method) {
        CheckoutStrategy strategy = strategies.get(method);
        if (strategy == null) {
            throw AppException.badRequest("Unsupported checkout method");
        }
        strategy.assertReady();
        return strategy;
    }
}
