package com.glowvera.web;

import com.glowvera.common.Normalizers;
import com.glowvera.domain.OrderCodes;
import com.glowvera.dto.request.CheckoutRequest;
import com.glowvera.dto.response.CheckoutResponse;
import com.glowvera.dto.response.OrderViews;
import com.glowvera.security.AuthenticatedUser;
import com.glowvera.service.OrderQueryService;
import com.glowvera.service.OrderService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.glowvera.common.AppException;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orders;
    private final OrderQueryService queries;

    public OrderController(OrderService orders, OrderQueryService queries) {
        this.orders = orders;
        this.queries = queries;
    }

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<CheckoutResponse>> checkout(
            @Valid @RequestBody CheckoutRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(orders.checkout(request, user)));
    }

    @GetMapping("/track")
    public ApiResponse<OrderViews.CustomerOrder> track(
            @RequestParam String code, @AuthenticationPrincipal AuthenticatedUser user) {
        String normalized = Normalizers.upper(code);
        if (!OrderCodes.isValid(normalized)) {
            throw AppException.badRequest("Validation failed",
                    List.of(java.util.Map.of("field", "code", "message", "Invalid order code")));
        }
        return ApiResponse.ok(queries.track(normalized, user.id()));
    }

    @GetMapping("/mine")
    public ApiResponse<List<OrderViews.CustomerOrder>> mine(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(queries.listMine(user.id()));
    }
}
