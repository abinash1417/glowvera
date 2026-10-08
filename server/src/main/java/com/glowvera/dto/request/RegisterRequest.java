package com.glowvera.dto.request;

import com.glowvera.common.Normalizers;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name is too short")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email address")
        @Size(max = 150)
        String email,

        @Pattern(regexp = Normalizers.PHONE_REGEX, message = Normalizers.PHONE_MESSAGE)
        String phone,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be 8 to 72 characters")
        @Pattern(regexp = ".*[A-Za-z].*", message = "Password must contain a letter")
        @Pattern(regexp = ".*\\d.*", message = "Password must contain a number")
        String password) {

    public RegisterRequest {
        name = Normalizers.trim(name);
        email = Normalizers.lower(email);
        phone = Normalizers.phone(phone);
    }
}
