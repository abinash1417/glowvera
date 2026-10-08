package com.glowvera.web;

import com.glowvera.checkout.ClientAction;
import com.glowvera.dto.request.OrderCodeRequest;
import com.glowvera.security.AuthenticatedUser;
import com.glowvera.service.WhatsAppService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/whatsapp")
public class WhatsAppController {

    private final WhatsAppService whatsApp;

    public WhatsAppController(WhatsAppService whatsApp) {
        this.whatsApp = whatsApp;
    }

    @PostMapping("/link")
    public ApiResponse<Map<String, ClientAction>> link(
            @Valid @RequestBody OrderCodeRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.ok(whatsApp.getLink(request.code(), user));
    }
}
