package com.glowvera.service;

import com.glowvera.common.AppException;
import com.glowvera.domain.OrderSnapshot;
import com.glowvera.dto.request.AdminOrderListQuery;
import com.glowvera.dto.response.OrderViews;
import com.glowvera.dto.response.PageResponse;
import com.glowvera.dto.response.Pagination;
import com.glowvera.entity.CustomerOrder;
import com.glowvera.mapper.OrderMapper;
import com.glowvera.repository.OrderRepository;
import com.glowvera.repository.OrderSpecifications;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrderQueryService {

    private final OrderRepository orders;

    public OrderQueryService(OrderRepository orders) {
        this.orders = orders;
    }


    public OrderSnapshot findOwnedSnapshot(String code, Long userId) {
        CustomerOrder order = orders.findByOrderCode(code)
                .filter(o -> o.getUser() != null && o.getUser().getId().equals(userId))
                .orElseThrow(() -> AppException.notFound("Order not found. Check the order code."));
        return OrderMapper.toSnapshot(order);
    }

    public OrderViews.CustomerOrder track(String code, Long userId) {
        return OrderMapper.toCustomerView(findOwnedSnapshot(code, userId));
    }

    public List<OrderViews.CustomerOrder> listMine(Long userId) {
        return orders.findTop50ByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(OrderMapper::toSnapshot)
                .map(OrderMapper::toCustomerView)
                .toList();
    }

    public PageResponse<OrderViews.AdminListItem> adminList(AdminOrderListQuery q) {
        PageRequest page = PageRequest.of(q.page() - 1, q.limit(),
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<CustomerOrder> result = orders.findAll(OrderSpecifications.adminSearch(q.status(), q.q()), page);
        return new PageResponse<>(
                result.getContent().stream().map(OrderMapper::toAdminListItem).toList(),
                Pagination.of(q.page(), q.limit(), result.getTotalElements()));
    }

    public OrderViews.AdminDetail adminGet(Long id) {
        CustomerOrder order = orders.findById(id).orElseThrow(() -> AppException.notFound("Order not found"));
        return OrderMapper.toAdminDetail(order);
    }
}
