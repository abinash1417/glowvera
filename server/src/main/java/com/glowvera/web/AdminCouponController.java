package com.glowvera.web;

import com.glowvera.dto.request.CouponRequest;
import com.glowvera.dto.response.CouponViews;
import com.glowvera.service.CouponService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/coupons")
public class AdminCouponController {

    private final CouponService coupons;

    public AdminCouponController(CouponService coupons) {
        this.coupons = coupons;
    }

    @GetMapping
    public ApiResponse<List<CouponViews.Item>> list() {
        return ApiResponse.ok(coupons.list());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CouponViews.Item>> create(@Valid @RequestBody CouponRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(coupons.create(request)));
    }

    @PutMapping("/{id}")
    public ApiResponse<CouponViews.Item> update(
            @PathVariable Long id, @Valid @RequestBody CouponRequest request) {
        return ApiResponse.ok(coupons.update(id, request));
    }
}
