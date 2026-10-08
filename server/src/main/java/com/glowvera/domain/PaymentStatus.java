package com.glowvera.domain;

import java.util.Optional;

public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    CANCELED,
    CHARGEDBACK;

    public static Optional<PaymentStatus> fromPayHereCode(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return switch (code.trim()) {
            case "2" -> Optional.of(SUCCESS);
            case "0" -> Optional.of(PENDING);
            case "-1" -> Optional.of(CANCELED);
            case "-2" -> Optional.of(FAILED);
            case "-3" -> Optional.of(CHARGEDBACK);
            default -> Optional.empty();
        };
    }
}
