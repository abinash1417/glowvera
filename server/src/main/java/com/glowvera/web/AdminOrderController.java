package com.glowvera.web;

import com.glowvera.dto.request.AdminOrderListQuery;
import com.glowvera.dto.request.ChangeStatusRequest;
import com.glowvera.dto.response.OrderViews;
import com.glowvera.dto.response.PageResponse;
import com.glowvera.security.AuthenticatedUser;
import com.glowvera.service.OrderQueryService;
import com.glowvera.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderQueryService queries;
    private final OrderService orders;

    public AdminOrderController(OrderQueryService queries, OrderService orders) {
        this.queries = queries;
        this.orders = orders;
    }

    @GetMapping
    public ApiResponse<PageResponse<OrderViews.AdminListItem>> list(@Valid AdminOrderListQuery query) {
        return ApiResponse.ok(queries.adminList(query));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderViews.AdminDetail> get(@PathVariable Long id) {
        return ApiResponse.ok(queries.adminGet(id));
    }

    @PostMapping("/{id}/status")
    public ApiResponse<OrderViews.AdminDetail> changeStatus(
            @PathVariable Long id, @Valid @RequestBody ChangeStatusRequest request,
            @AuthenticationPrincipal AuthenticatedUser admin) {
        return ApiResponse.ok(orders.changeStatusByAdmin(id, request.status(), admin.email(), request.note()));
    }
}
