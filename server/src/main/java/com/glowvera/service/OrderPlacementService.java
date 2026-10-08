package com.glowvera.service;

import com.glowvera.common.AppException;
import com.glowvera.domain.CartRules;
import com.glowvera.domain.CartRules.Line;
import com.glowvera.domain.CartRules.LineStatus;
import com.glowvera.domain.CartRules.PricedLine;
import com.glowvera.domain.OrderCodes;
import com.glowvera.domain.OrderSnapshot;
import com.glowvera.domain.OrderStatus;
import com.glowvera.domain.PaymentMethod;
import com.glowvera.dto.request.CheckoutRequest;
import com.glowvera.entity.CustomerOrder;
import com.glowvera.entity.OrderItem;
import com.glowvera.entity.OrderStatusHistory;
import com.glowvera.entity.ProductVariant;
import com.glowvera.mapper.OrderMapper;
import com.glowvera.repository.OrderRepository;
import com.glowvera.repository.OrderStatusHistoryRepository;
import com.glowvera.repository.ProductVariantRepository;
import com.glowvera.repository.SellableQty;
import com.glowvera.repository.StockBatchRepository;
import com.glowvera.repository.UserRepository;
import com.glowvera.security.AuthenticatedUser;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;


@Service
public class OrderPlacementService {

    public record StockProblem(Long variantId, String name, LineStatus reason, int available) {
    }

    public record PlaceOrderCommand(
            PaymentMethod method,
            OrderStatus initialStatus,
            Instant reservationExpiresAt,
            List<Line> lines,
            CheckoutRequest.CustomerDetails customer,
            String district,
            long shippingCents,
            String notes,
            String couponCode,
            AuthenticatedUser user) {
    }

    private final ProductVariantRepository variants;
    private final StockBatchRepository batches;
    private final OrderRepository orders;
    private final OrderStatusHistoryRepository history;
    private final UserRepository users;
    private final CouponService coupons;
    private final Clock clock;

    public OrderPlacementService(ProductVariantRepository variants, StockBatchRepository batches,
                                 OrderRepository orders, OrderStatusHistoryRepository history,
                                 UserRepository users, CouponService coupons, Clock clock) {
        this.coupons = coupons;
        this.variants = variants;
        this.batches = batches;
        this.orders = orders;
        this.history = history;
        this.users = users;
        this.clock = clock;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OrderSnapshot place(PlaceOrderCommand cmd) {
        List<Long> ids = cmd.lines().stream().map(Line::variantId).toList();

        // 1. LOCK (ascending id order, so two checkouts can never deadlock)
        Map<Long, ProductVariant> byId = variants.lockAllByIds(ids).stream()
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));

        // 2. READ fresh sellable (unexpired) stock
        Map<Long, Integer> sellable = batches.sellableByVariant(ids, LocalDate.now(clock)).stream()
                .collect(Collectors.toMap(SellableQty::variantId, s -> s.quantity().intValue()));

        // 3. CHECK every line
        List<StockProblem> problems = new ArrayList<>();
        List<OrderItem> items = new ArrayList<>();
        List<PricedLine> priced = new ArrayList<>();

        for (Line line : cmd.lines()) {
            ProductVariant v = byId.get(line.variantId());
            int sellableQty = sellable.getOrDefault(line.variantId(), 0);
            boolean purchasable = v != null && v.isActive() && v.getProduct().isActive();
            LineStatus status = CartRules.evaluateLine(
                    purchasable, v == null ? 0 : v.getReservedQty(), sellableQty, line.quantity());

            if (status != LineStatus.OK) {
                problems.add(new StockProblem(
                        line.variantId(),
                        v == null ? null : v.getProduct().getName() + " (" + v.getName() + ")",
                        status,
                        v == null ? 0 : CartRules.availableForSale(v.getReservedQty(), sellableQty)));
                continue;
            }

            OrderItem item = new OrderItem();
            item.setVariant(v);
            item.setProductName(v.getProduct().getName());      // snapshots
            item.setVariantName(v.getName());
            item.setUnitPriceCents(v.getPriceCents());
            item.setQuantity(line.quantity());
            item.setLineTotalCents(v.getPriceCents() * line.quantity());
            items.add(item);
            priced.add(new PricedLine(v.getPriceCents(), line.quantity()));
        }

        if (!problems.isEmpty()) {
            throw AppException.conflict("Some items in your cart are no longer available", problems);
        }

        // 3b. COUPON (checked before anything is reserved; the coupon row is locked until commit)
        CartRules.Totals totals = CartRules.calculateTotals(priced, cmd.shippingCents());
        long discountCents = 0;
        String appliedCoupon = null;
        if (cmd.couponCode() != null) {
            CouponService.Applied applied =
                    coupons.applyForCheckout(cmd.couponCode(), totals.subtotalCents(), cmd.user().id());
            discountCents = applied.discountCents();
            appliedCoupon = applied.coupon().getCode();
        }

        // 4. RESERVE
        for (OrderItem item : items) {
            item.getVariant().reserve(item.getQuantity());
        }

        // 5. SAVE
        CheckoutRequest.CustomerDetails c = cmd.customer();

        CustomerOrder order = new CustomerOrder();
        order.setOrderCode(OrderCodes.temporary());
        order.setUser(users.getReferenceById(cmd.user().id()));
        order.setCustomerName(c.name());
        order.setCustomerEmail(c.email());
        order.setCustomerPhone(c.phone());
        order.setAddressLine(c.addressLine());
        order.setCity(c.city());
        order.setDistrict(cmd.district());
        order.setSubtotalCents(totals.subtotalCents());
        order.setShippingCents(totals.shippingCents());
        order.setDiscountCents(discountCents);
        order.setCouponCode(appliedCoupon);
        order.setTotalCents(totals.subtotalCents() - discountCents + totals.shippingCents());
        order.setCurrency("LKR");
        order.setPaymentMethod(cmd.method());
        order.setStatus(cmd.initialStatus());
        order.setReservationExpiresAt(cmd.reservationExpiresAt());
        order.setNotes(cmd.notes());
        items.forEach(order::addItem);

        orders.saveAndFlush(order);   // INSERT now, so we know the generated id
        order.setOrderCode(OrderCodes.format(order.getId(), LocalDate.now(clock).getYear()));

        history.save(new OrderStatusHistory(order, null, cmd.initialStatus(), cmd.user().email(), "Order placed"));

        return OrderMapper.toSnapshot(order);
    }
}
