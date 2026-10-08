package com.glowvera.repository;

import com.glowvera.domain.OrderStatus;
import com.glowvera.entity.CustomerOrder;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class OrderSpecifications {

    private OrderSpecifications() {
    }

    public static Specification<CustomerOrder> adminSearch(OrderStatus status, String q) {
        return (root, query, cb) -> {
            List<Predicate> all = new ArrayList<>();
            if (status != null) {
                all.add(cb.equal(root.get("status"), status));
            }
            if (ProductSpecifications.hasText(q)) {
                String pattern = ProductSpecifications.like(q);
                all.add(cb.or(
                        cb.like(cb.lower(root.get("orderCode")), pattern, '\\'),
                        cb.like(cb.lower(root.get("customerName")), pattern, '\\'),
                        cb.like(cb.lower(root.get("customerPhone")), pattern, '\\'),
                        cb.like(cb.lower(root.get("customerEmail")), pattern, '\\')));
            }
            return cb.and(all.toArray(new Predicate[0]));
        };
    }
}
