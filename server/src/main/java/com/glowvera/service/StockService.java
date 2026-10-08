package com.glowvera.service;

import com.glowvera.common.AppException;
import com.glowvera.entity.CustomerOrder;
import com.glowvera.entity.OrderItem;
import com.glowvera.entity.OrderItemBatch;
import com.glowvera.entity.ProductVariant;
import com.glowvera.entity.StockBatch;
import com.glowvera.repository.OrderItemBatchRepository;
import com.glowvera.repository.ProductVariantRepository;
import com.glowvera.repository.StockBatchRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockService {

    private final ProductVariantRepository variants;
    private final StockBatchRepository batches;
    private final OrderItemBatchRepository allocations;
    private final Clock clock;

    public StockService(ProductVariantRepository variants, StockBatchRepository batches,
                        OrderItemBatchRepository allocations, Clock clock) {
        this.variants = variants;
        this.batches = batches;
        this.allocations = allocations;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void commit(CustomerOrder order) {
        LocalDate today = LocalDate.now(clock);
        List<OrderItem> items = sortedItems(order);
        Map<Long, ProductVariant> locked = lockVariantsOf(items);
        List<OrderItemBatch> rows = new ArrayList<>();

        for (OrderItem item : items) {
            List<StockBatch> usable = batches.lockPositiveBatches(item.getVariant().getId()).stream()
                    .filter(b -> !b.isExpiredOn(today))
                    .toList();

            int remaining = item.getQuantity();
            for (StockBatch batch : usable) {
                if (remaining == 0) {
                    break;
                }
                int take = Math.min(batch.getQuantity(), remaining);
                batch.take(take);
                rows.add(new OrderItemBatch(item, batch, take));
                remaining -= take;
            }

            if (remaining > 0) {
                throw AppException.conflict("Not enough unexpired stock for \"" + item.getProductName()
                        + " (" + item.getVariantName() + ")\". Write off expired batches or receive new stock,"
                        + " then try again.");
            }
            locked.get(item.getVariant().getId()).commit(item.getQuantity());
        }
        allocations.saveAll(rows);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void release(CustomerOrder order) {
        List<OrderItem> items = sortedItems(order);
        Map<Long, ProductVariant> locked = lockVariantsOf(items);
        for (OrderItem item : items) {
            locked.get(item.getVariant().getId()).release(item.getQuantity());
        }
    }

    // Paid order cancelled: put the units back into the SAME batches they came from. //
    @Transactional(propagation = Propagation.MANDATORY)
    public void restock(CustomerOrder order) {
        Map<Long, ProductVariant> locked = lockVariantsOf(sortedItems(order));
        Map<Long, Integer> perVariant = new HashMap<>();

        for (OrderItemBatch a : allocations.findByOrderId(order.getId())) {
            a.getBatch().putBack(a.getQuantity());
            perVariant.merge(a.getOrderItem().getVariant().getId(), a.getQuantity(), Integer::sum);
        }
        perVariant.forEach((variantId, qty) -> locked.get(variantId).restock(qty));
    }

    private static List<OrderItem> sortedItems(CustomerOrder order) {
        return order.getItems().stream()
                .sorted(Comparator.comparing((OrderItem i) -> i.getVariant().getId()))
                .toList();
    }

    // Locks in ascending id order (the same order everywhere), which prevents deadlocks. //
    private Map<Long, ProductVariant> lockVariantsOf(List<OrderItem> items) {
        List<Long> ids = items.stream().map(i -> i.getVariant().getId()).distinct().sorted().toList();
        return variants.lockAllByIds(ids).stream()
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));
    }
}
