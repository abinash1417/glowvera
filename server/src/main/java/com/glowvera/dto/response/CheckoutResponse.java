package com.glowvera.dto.response;

import com.glowvera.checkout.ClientAction;

public record CheckoutResponse(OrderViews.CustomerOrder order, ClientAction clientAction) {
}
