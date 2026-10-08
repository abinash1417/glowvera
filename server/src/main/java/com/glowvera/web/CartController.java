package com.glowvera.web;

import com.glowvera.dto.request.QuoteRequest;
import com.glowvera.dto.response.CartViews;
import com.glowvera.security.AuthenticatedUser;
import com.glowvera.service.CartService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cart;

    public CartController(CartService cart) {
        this.cart = cart;
    }

    @PostMapping("/quote")
    public ApiResponse<CartViews.Quote> quote(
            @Valid @RequestBody QuoteRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        // user is null for guests: they can still see prices, but coupons need a login
        return ApiResponse.ok(cart.quote(request, user == null ? null : user.id()));
    }

    @GetMapping("/districts")
    public ApiResponse<List<CartViews.District>> districts() {
        return ApiResponse.ok(cart.listDistricts());
    }
}
