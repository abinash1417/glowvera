package com.glowvera.service;

import com.glowvera.common.AppException;
import com.glowvera.domain.CartRules;
import com.glowvera.domain.CartRules.Line;
import com.glowvera.domain.CartRules.LineStatus;
import com.glowvera.domain.CartRules.PricedLine;
import com.glowvera.dto.request.CartItemRequest;
import com.glowvera.dto.request.QuoteRequest;
import com.glowvera.dto.response.CartViews;
import com.glowvera.entity.ProductVariant;
import com.glowvera.repository.ProductVariantRepository;
import com.glowvera.repository.SellableQty;
import com.glowvera.repository.ShippingRateRepository;
import com.glowvera.repository.StockBatchRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CartService {

    private final ProductVariantRepository variants;
    private final StockBatchRepository batches;
    private final ShippingRateRepository shippingRates;
    private final CouponService coupons;
    private final Clock clock;

    public CartService(ProductVariantRepository variants, StockBatchRepository batches,
                       ShippingRateRepository shippingRates, CouponService coupons, Clock clock) {
        this.coupons = coupons;
        this.variants = variants;
        this.batches = batches;
        this.shippingRates = shippingRates;
        this.clock = clock;
    }

    public List<CartViews.District> listDistricts() {
        return shippingRates.findAllByOrderByDistrictAsc().stream()
                .map(r -> new CartViews.District(r.getDistrict(), r.getFeeCents()))
                .toList();
    }

    public CartViews.Quote quote(QuoteRequest request, Long userId) {
        List<Line> lines = CartRules.mergeItems(request.items().stream().map(CartItemRequest::toLine).toList());
        List<Long> ids = lines.stream().map(Line::variantId).toList();

        Long shipping = null;
        if (request.district() != null) {
            shipping = shippingRates.findByDistrict(request.district())
                    .orElseThrow(() -> AppException.badRequest("We do not deliver to this district yet"))
                    .getFeeCents();
        }

        Map<Long, ProductVariant> byId = variants.findAllById(ids).stream()
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));
        Map<Long, Integer> sellable = batches.sellableByVariant(ids, LocalDate.now(clock)).stream()
                .collect(Collectors.toMap(SellableQty::variantId, s -> s.quantity().intValue()));

        List<CartViews.QuoteLine> result = new ArrayList<>();
        List<PricedLine> priced = new ArrayList<>();
        boolean canCheckout = true;

        for (Line line : lines) {
            ProductVariant v = byId.get(line.variantId());
            int sellableQty = sellable.getOrDefault(line.variantId(), 0);
            boolean purchasable = v != null && v.isActive() && v.getProduct().isActive();
            LineStatus status = CartRules.evaluateLine(
                    purchasable, v == null ? 0 : v.getReservedQty(), sellableQty, line.quantity());

            if (status != LineStatus.OK) {
                canCheckout = false;
            }
            if (status == LineStatus.UNAVAILABLE) {
                result.add(new CartViews.QuoteLine(line.variantId(), null, null, null,
                        line.quantity(), null, null, status));
                continue;
            }
            long lineTotal = v.getPriceCents() * line.quantity();
            result.add(new CartViews.QuoteLine(
                    v.getId(), v.getProduct().getName(), v.getName(), v.getPriceCents(), line.quantity(),
                    lineTotal, CartRules.availableForSale(v.getReservedQty(), sellableQty), status));
            priced.add(new PricedLine(v.getPriceCents(), line.quantity()));
        }

        CartRules.Totals totals = CartRules.calculateTotals(priced, shipping == null ? 0 : shipping);

        long discount = 0;
        String appliedCode = null;
        String couponError = null;
        if (request.couponCode() != null) {
            try {
                CouponService.Applied applied = coupons.preview(request.couponCode(), totals.subtotalCents(), userId);
                discount = applied.discountCents();
                appliedCode = applied.coupon().getCode();
            } catch (AppException e) {
                couponError = e.getMessage();   // the cart still works; we just explain why the coupon was refused
            }
        }
        return new CartViews.Quote(result, totals.subtotalCents(), shipping, totals.totalCents() - discount,
                canCheckout, appliedCode, discount, couponError);
    }
}
