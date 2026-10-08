package com.glowvera.payment;

import com.glowvera.checkout.ClientAction;
import com.glowvera.config.AppProperties;
import com.glowvera.domain.Money;
import com.glowvera.domain.OrderSnapshot;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PayHereFormBuilder {

    private static final String SANDBOX_URL = "https://sandbox.payhere.lk/pay/checkout";
    private static final String LIVE_URL = "https://www.payhere.lk/pay/checkout";

    private final AppProperties props;
    private final int serverPort;

    public PayHereFormBuilder(AppProperties props, @Value("${server.port:5000}") int serverPort) {
        this.props = props;
        this.serverPort = serverPort;
    }

    public PayHereSigner signer() {
        return new PayHereSigner(props.payhere().merchantId(), props.payhere().merchantSecret());
    }

    public ClientAction.PayHereForm build(OrderSnapshot order) {
        String apiUrl = props.publicApiUrl() == null || props.publicApiUrl().isBlank()
                ? "http://localhost:" + serverPort
                : props.publicApiUrl();
        apiUrl = apiUrl.replaceAll("/+$", "");

        String[] nameParts = order.customerName().trim().split("\\s+", 2);
        String firstName = nameParts[0];
        String lastName = nameParts.length > 1 ? nameParts[1] : firstName;
        String amount = Money.formatAmount(order.totalCents());
        String clientUrl = props.clientUrl();

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("merchant_id", props.payhere().merchantId());
        fields.put("return_url", clientUrl + "/payment/return?order=" + order.orderCode());
        fields.put("cancel_url", clientUrl + "/payment/cancelled?order=" + order.orderCode());
        fields.put("notify_url", apiUrl + "/api/payments/payhere/notify");
        fields.put("order_id", order.orderCode());
        fields.put("items", "Glowvera order " + order.orderCode());
        fields.put("currency", order.currency());
        fields.put("amount", amount);
        fields.put("first_name", firstName);
        fields.put("last_name", lastName);
        fields.put("email", order.customerEmail());
        fields.put("phone", order.customerPhone());
        fields.put("address", order.addressLine());
        fields.put("city", order.city());
        fields.put("country", "Sri Lanka");
        // the hash is computed from the SAME amount string that is sent
        fields.put("hash", signer().checkoutHash(order.orderCode(), amount, order.currency()));

        return ClientAction.PayHereForm.of(props.payhere().sandbox() ? SANDBOX_URL : LIVE_URL, fields);
    }
}
