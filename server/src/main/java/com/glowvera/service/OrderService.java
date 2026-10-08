package com.glowvera.service;

import com.glowvera.checkout.CheckoutStrategy;
import com.glowvera.checkout.CheckoutStrategyRegistry;
import com.glowvera.checkout.ClientAction;
import com.glowvera.common.AppException;
import com.glowvera.domain.CartRules;
import com.glowvera.domain.OrderSnapshot;
import com.glowvera.domain.OrderStatus;
import com.glowvera.dto.request.CheckoutRequest;
import com.glowvera.dto.request.CartItemRequest;
import com.glowvera.dto.response.CheckoutResponse;
import com.glowvera.dto.response.OrderViews;
import com.glowvera.entity.ShippingRate;
import com.glowvera.mapper.OrderMapper;
import com.glowvera.repository.ShippingRateRepository;
import com.glowvera.security.AuthenticatedUser;
import com.glowvera.service.OrderPlacementService.PlaceOrderCommand;
import com.glowvera.service.OrderTransitionService.TransitionRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;


@Service
public class OrderService {

    private final CheckoutStrategyRegistry strategies;
    private final ShippingRateRepository shippingRates;
    private final OrderPlacementService placement;
    private final OrderTransitionService transitions;
    private final OrderQueryService queries;
    private final Clock clock;

    public OrderService(CheckoutStrategyRegistry strategies, ShippingRateRepository shippingRates,
                        OrderPlacementService placement, OrderTransitionService transitions,
                        OrderQueryService queries, Clock clock) {
        this.strategies = strategies;
        this.shippingRates = shippingRates;
        this.placement = placement;
        this.transitions = transitions;
        this.queries = queries;
        this.clock = clock;
    }

    public CheckoutResponse checkout(CheckoutRequest request, AuthenticatedUser user) {
        // Fails BEFORE anything is reserved if the chosen method is not configured.
        CheckoutStrategy strategy = strategies.ready(request.method());

        List<CartRules.Line> lines = CartRules.mergeItems(
                request.items().stream().map(CartItemRequest::toLine).toList());

        ShippingRate rate = shippingRates.findByDistrict(request.customer().district())
                .orElseThrow(() -> AppException.badRequest("We do not deliver to this district yet"));

        Instant expiresAt = Instant.now(clock).plus(strategy.reservationWindow());

        OrderSnapshot order = placement.place(new PlaceOrderCommand(
                strategy.method(), strategy.initialStatus(), expiresAt, lines,
                request.customer(), rate.getDistrict(), rate.getFeeCents(), request.notes(),
                request.couponCode(), user));

        // The strategy decides what the browser does next (PayHere form or WhatsApp link).
        ClientAction action = strategy.buildClientAction(order);
        return new CheckoutResponse(OrderMapper.toCustomerView(order), action);
    }

    public OrderViews.AdminDetail changeStatusByAdmin(Long id, OrderStatus status, String adminEmail, String note) {
        transitions.transition(id, status, TransitionRequest.byAdmin(adminEmail, note));
        return queries.adminGet(id);
    }
}
