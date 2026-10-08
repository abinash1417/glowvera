package com.glowvera.dto.request;

import com.glowvera.common.Normalizers;
import com.glowvera.domain.OrderCodes;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record OrderCodeRequest(
        @NotBlank(message = "Order code is required")
        @Pattern(regexp = OrderCodes.PATTERN, message = "Invalid order code")
        String code) {

    public OrderCodeRequest {
        code = Normalizers.upper(code);
    }
}
