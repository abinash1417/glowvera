package com.glowvera.dto.request;

import com.glowvera.common.Normalizers;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

public record CreateBatchRequest(
        @NotBlank(message = "Batch code is required")
        @Pattern(regexp = "^[A-Za-z0-9-]{2,50}$", message = "Batch code must be 2-50 letters, numbers or dashes")
        String batchCode,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be at least 1")
        @Max(value = 100_000, message = "Quantity is too large")
        Integer quantity,

        @NotNull(message = "Expiry date is required (YYYY-MM-DD)")
        @Future(message = "Expiry date must be in the future")
        LocalDate expiryDate) {

    public CreateBatchRequest {
        batchCode = Normalizers.upper(batchCode);
    }
}
