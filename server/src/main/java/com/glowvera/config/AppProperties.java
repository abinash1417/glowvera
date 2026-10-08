package com.glowvera.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;


@Validated
@ConfigurationProperties(prefix = "glowvera")
public record AppProperties(
        @NotBlank String environment,
        @NotBlank String clientUrl,
        String publicApiUrl,
        @Pattern(regexp = "^$|^\\d{10,15}$",
                message = "WHATSAPP_NUMBER must be digits only with country code, e.g. 94771234567")
        String whatsappNumber,
        boolean seedDemoData,
        @Valid Jwt jwt,
        @Valid PayHere payhere,
        @Valid Admin admin) {

    public record Jwt(
            @Size(min = 32, message = "JWT_SECRET must be at least 32 characters") String secret,
            @Min(5) @Max(43200) int expiresMinutes) {
    }

    public record PayHere(String merchantId, String merchantSecret, boolean sandbox) {
        public boolean configured() {
            return merchantId != null && !merchantId.isBlank()
                    && merchantSecret != null && !merchantSecret.isBlank();
        }
    }

    public record Admin(String name, String email, String password) {
    }

    public boolean production() {
        return "production".equalsIgnoreCase(environment);
    }

    public boolean whatsappConfigured() {
        return whatsappNumber != null && !whatsappNumber.isBlank();
    }
}
