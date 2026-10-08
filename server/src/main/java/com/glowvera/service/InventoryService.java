package com.glowvera.service;

import com.glowvera.common.AppException;
import com.glowvera.domain.StockMath;
import com.glowvera.domain.StockMath.StockStatus;
import com.glowvera.dto.request.CreateBatchRequest;
import com.glowvera.dto.response.InventoryViews;
import com.glowvera.entity.ProductVariant;
import com.glowvera.entity.StockBatch;
import com.glowvera.mapper.StockMapper;
import com.glowvera.repository.ProductVariantRepository;
import com.glowvera.repository.StockBatchRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final ProductVariantRepository variants;
    private final StockBatchRepository batches;
    private final Clock clock;

    @PersistenceContext
    private EntityManager em;

    public InventoryService(ProductVariantRepository variants, StockBatchRepository batches, Clock clock) {
        this.variants = variants;
        this.batches = batches;
        this.clock = clock;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public InventoryViews.ReceiveBatchResult receiveBatch(Long variantId, CreateBatchRequest r) {
        ProductVariant variant = variants.lockById(variantId)
                .orElseThrow(() -> AppException.notFound("Variant not found"));

        if (batches.findByVariantIdAndBatchCode(variantId, r.batchCode()).isPresent()) {
            throw AppException.conflict("Batch " + r.batchCode() + " already exists for this variant");
        }
        StockBatch batch = batches.saveAndFlush(new StockBatch(variant, r.batchCode(), r.quantity(), r.expiryDate()));
        variant.receive(r.quantity());   // rule: variant stock always equals the sum of its batches

        return new InventoryViews.ReceiveBatchResult(
                StockMapper.toBatchView(batch, LocalDate.now(clock)), StockMapper.toVariantView(variant));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public InventoryViews.WriteOffResult writeOff(Long batchId) {
        StockBatch found = batches.findById(batchId).orElseThrow(() -> AppException.notFound("Batch not found"));
        ProductVariant variant = variants.lockById(found.getVariant().getId()).orElseThrow();
        em.refresh(found);   // re-read AFTER taking the lock: a checkout may have changed it

        if (found.getQuantity() == 0) {
            throw AppException.badRequest("This batch is already empty");
        }
        if (variant.getStockQty() - found.getQuantity() < variant.getReservedQty()) {
            throw AppException.conflict("Cannot write off: some of this stock is reserved by unpaid orders."
                    + " Try again after they are paid or expire.");
        }
        int units = found.getQuantity();
        found.setQuantity(0);
        variant.writeOff(units);
        return new InventoryViews.WriteOffResult(units, StockMapper.toVariantView(variant));
    }

    @Transactional(readOnly = true)
    public InventoryViews.Alerts getAlerts(int days) {
        LocalDate today = LocalDate.now(clock);

        List<InventoryViews.StockAlert> stockRows = variants.findAllActiveWithProduct().stream()
                .map(v -> new InventoryViews.StockAlert(
                        v.getId(), v.getSku(), v.getName(), v.getProduct().getId(), v.getProduct().getName(),
                        v.availableQty(), v.getLowStockThreshold(),
                        StockMath.stockStatus(v.getStockQty(), v.getReservedQty(), v.getLowStockThreshold())))
                .filter(r -> r.stockStatus() != StockStatus.OK)
                .toList();

        List<InventoryViews.BatchAlert> batchRows = batches.findExpiringBy(today.plusDays(days)).stream()
                .map(b -> new InventoryViews.BatchAlert(
                        b.getId(), b.getBatchCode(), b.getQuantity(), b.getExpiryDate(),
                        StockMath.daysUntil(b.getExpiryDate(), today),
                        b.getVariant().getId(), b.getVariant().getSku(), b.getVariant().getName(),
                        b.getVariant().getProduct().getId(), b.getVariant().getProduct().getName()))
                .toList();

        List<InventoryViews.StockAlert> out = stockRows.stream()
                .filter(r -> r.stockStatus() == StockStatus.OUT_OF_STOCK).toList();
        List<InventoryViews.StockAlert> low = stockRows.stream()
                .filter(r -> r.stockStatus() == StockStatus.LOW_STOCK).toList();
        List<InventoryViews.BatchAlert> expired = batchRows.stream().filter(r -> r.daysToExpiry() < 0).toList();
        List<InventoryViews.BatchAlert> near = batchRows.stream().filter(r -> r.daysToExpiry() >= 0).toList();

        return new InventoryViews.Alerts(days,
                new InventoryViews.Summary(out.size(), low.size(), near.size(), expired.size()),
                out, low, near, expired);
    }
}
