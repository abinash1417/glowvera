package com.glowvera.web;

import com.glowvera.checkout.ClientAction;
import com.glowvera.dto.request.OrderCodeRequest;
import com.glowvera.security.AuthenticatedUser;
import com.glowvera.service.PaymentService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments/payhere")
public class PaymentController {

    private final PaymentService payments;

    public PaymentController(PaymentService payments) {
        this.payments = payments;
    }

    @PostMapping(value = "/notify", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> notifyPayment(@RequestParam Map<String, String> body) {
        payments.handlePayHereNotification(body);
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body("OK");
    }

    @PostMapping("/retry")
    public ApiResponse<Map<String, ClientAction>> retry(
            @Valid @RequestBody OrderCodeRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(payments.retryPayHere(request.code(), user));
    }
}
