package com.glowvera.service;

import com.glowvera.checkout.CheckoutStrategyRegistry;
import com.glowvera.checkout.ClientAction;
import com.glowvera.common.AppException;
import com.glowvera.common.ErrorCode;
import com.glowvera.config.AppProperties;
import com.glowvera.domain.Money;
import com.glowvera.domain.OrderSnapshot;
import com.glowvera.domain.OrderStatus;
import com.glowvera.domain.PaymentMethod;
import com.glowvera.domain.PaymentStatus;
import com.glowvera.entity.CustomerOrder;
import com.glowvera.entity.OrderStatusHistory;
import com.glowvera.entity.Payment;
import com.glowvera.payment.PayHereSigner;
import com.glowvera.repository.OrderRepository;
import com.glowvera.repository.OrderStatusHistoryRepository;
import com.glowvera.repository.PaymentRepository;
import com.glowvera.security.AuthenticatedUser;
import com.glowvera.service.OrderTransitionService.TransitionRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.OptionalLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;


@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final Set<String> CARD_FIELDS = Set.of("card_holder_name", "card_no", "card_expiry");
    private static final List<String> REQUIRED = List.of(
            "merchant_id", "order_id", "payment_id", "payhere_amount", "payhere_currency", "status_code", "md5sig");

    private final AppProperties props;
    private final OrderRepository orders;
    private final OrderStatusHistoryRepository history;
    private final PaymentRepository payments;
    private final OrderTransitionService transitions;
    private final OrderQueryService queries;
    private final CheckoutStrategyRegistry strategies;
    private final Clock clock;

    public PaymentService(AppProperties props, OrderRepository orders, OrderStatusHistoryRepository history,
                          PaymentRepository payments, OrderTransitionService transitions,
                          OrderQueryService queries, CheckoutStrategyRegistry strategies, Clock clock) {
        this.props = props;
        this.orders = orders;
        this.history = history;
        this.payments = payments;
        this.transitions = transitions;
        this.queries = queries;
        this.strategies = strategies;
        this.clock = clock;
    }

    public void handlePayHereNotification(Map<String, String> body) {
        if (!props.payhere().configured()) {
            throw new AppException(ErrorCode.PAYMENT_UNAVAILABLE, "Payments are not configured");
        }
        requireFields(body);

        // 1. AUTHENTICITY
        PayHereSigner signer = new PayHereSigner(props.payhere().merchantId(), props.payhere().merchantSecret());
        String expected = signer.notificationSignature(
                body.get("merchant_id"), body.get("order_id"), body.get("payhere_amount"),
                body.get("payhere_currency"), body.get("status_code"));
        boolean authentic = PayHereSigner.signaturesMatch(expected, body.get("md5sig"))
                && body.get("merchant_id").equals(props.payhere().merchantId());
        if (!authentic) {
            log.warn("Rejected PayHere notification with bad signature (order {})", body.get("order_id"));
            throw AppException.badRequest("Invalid signature");
        }

        PaymentStatus paymentStatus = PaymentStatus.fromPayHereCode(body.get("status_code"))
                .orElseThrow(() -> AppException.badRequest("Unknown status code"));

        CustomerOrder order = orders.findByOrderCode(body.get("order_id"))
                .filter(o -> o.getPaymentMethod() == PaymentMethod.PAYHERE)
                .orElseThrow(() -> AppException.notFound("Order not found"));

        // 2. IDEMPOTENCY
        String paymentId = body.get("payment_id");
        if (payments.existsByPayherePaymentId(paymentId)) {
            return;
        }

        // 3. INTEGRITY
        OptionalLong amount = Money.parseAmountToCents(body.get("payhere_amount"));
        if (amount.isEmpty()) {
            throw AppException.badRequest("Invalid amount");
        }
        long amountCents = amount.getAsLong();
        boolean amountOk = amountCents == order.getTotalCents()
                && body.get("payhere_currency").equals(order.getCurrency());

        // 4. ACT
        if (paymentStatus == PaymentStatus.SUCCESS) {
            if (!amountOk) {
                log.error("Amount mismatch on {}: got {} {}", order.getOrderCode(),
                        body.get("payhere_amount"), body.get("payhere_currency"));
                flag(order.getId(), "PayHere payment " + paymentId + " has wrong amount/currency ("
                        + body.get("payhere_amount") + " " + body.get("payhere_currency") + "). Review manually.");
            } else {
                boolean moved = transitions.transition(order.getId(), OrderStatus.PAID,
                        TransitionRequest.guarded("payhere", "PayHere payment " + paymentId,
                                o -> o.getStatus() == OrderStatus.PENDING_PAYMENT));
                if (!moved) {
                    flag(order.getId(), "PayHere payment " + paymentId
                            + " arrived but the order was not awaiting payment. A refund may be needed.");
                }
            }
        } else if (paymentStatus == PaymentStatus.CHARGEDBACK) {
            flag(order.getId(), "PayHere chargeback for payment " + paymentId + ". Review manually.");
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setProvider("PAYHERE");
        payment.setPayherePaymentId(paymentId);
        payment.setStatusCode(Integer.valueOf(body.get("status_code").trim()));
        payment.setStatus(paymentStatus);
        payment.setAmountCents(amountCents);
        payment.setCurrency(body.get("payhere_currency"));
        payment.setSignatureValid(true);
        payment.setRawPayload(safePayload(body));
        try {
            payments.saveAndFlush(payment);
        } catch (DataIntegrityViolationException e) {
            // Two identical notifications at the same instant: the UNIQUE key stops the second one.
            log.info("Duplicate PayHere notification ignored: {}", paymentId);
        }
    }

    public Map<String, ClientAction> retryPayHere(String code, AuthenticatedUser user) {
        OrderSnapshot order = queries.findOwnedSnapshot(code, user.id());
        if (order.paymentMethod() != PaymentMethod.PAYHERE || order.status() != OrderStatus.PENDING_PAYMENT) {
            throw AppException.conflict("This order is not waiting for payment");
        }
        if (order.reservationExpiresAt() != null && order.reservationExpiresAt().isBefore(Instant.now(clock))) {
            throw AppException.conflict("This order has expired. Please place a new order.");
        }
        return Map.of("clientAction", strategies.ready(PaymentMethod.PAYHERE).buildClientAction(order));
    }

    private void flag(Long orderId, String note) {
        CustomerOrder order = orders.findById(orderId).orElseThrow();
        history.save(new OrderStatusHistory(order, order.getStatus(), order.getStatus(), "payhere", note));
    }

    private static void requireFields(Map<String, String> body) {
        List<Map<String, String>> missing = REQUIRED.stream()
                .filter(f -> body.get(f) == null || body.get(f).isBlank() || body.get(f).length() > 64)
                .map(f -> Map.of("field", f, "message", "Missing or invalid"))
                .toList();
        if (!missing.isEmpty()) {
            throw AppException.badRequest("Validation failed", missing);
        }
    }

    private static Map<String, String> safePayload(Map<String, String> body) {
        Map<String, String> copy = new HashMap<>(body);
        CARD_FIELDS.forEach(copy::remove);
        return copy;
    }
}
