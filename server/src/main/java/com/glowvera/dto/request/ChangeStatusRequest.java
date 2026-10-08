package com.glowvera.dto.request;

import com.glowvera.common.Normalizers;
import com.glowvera.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangeStatusRequest(
        @NotNull(message = "status is required") OrderStatus status,
        @Size(max = 255, message = "Note is too long") String note) {

    public ChangeStatusRequest {
        note = Normalizers.trimToNull(note);
    }
}
