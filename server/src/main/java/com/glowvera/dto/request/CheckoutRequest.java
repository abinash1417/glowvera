package com.glowvera.dto.request;

import com.glowvera.common.Normalizers;
import com.glowvera.domain.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CheckoutRequest(
        @NotNull(message = "Choose a payment method") PaymentMethod method,

        @NotEmpty(message = "Your cart is empty")
        @Size(max = 30, message = "Too many different items in one order")
        List<@Valid CartItemRequest> items,

        @NotNull(message = "Customer details are required") @Valid CustomerDetails customer,

        @Size(max = 500, message = "Notes are too long") String notes,

        @Size(max = 40, message = "Coupon code is too long") String couponCode) {

    public CheckoutRequest {
        notes = Normalizers.trimToNull(notes);
        couponCode = Normalizers.trimToNull(Normalizers.upper(couponCode));
    }

    public record CustomerDetails(
            @NotBlank(message = "Enter your name")
            @Size(min = 2, max = 100, message = "Enter your name")
            String name,

            @NotBlank(message = "Email is required")
            @Email(message = "Enter a valid email address")
            @Size(max = 150)
            String email,

            @NotBlank(message = "Phone is required")
            @Pattern(regexp = Normalizers.PHONE_REGEX, message = Normalizers.PHONE_MESSAGE)
            String phone,

            @NotBlank(message = "Enter your full address")
            @Size(min = 5, max = 255, message = "Enter your full address")
            String addressLine,

            @NotBlank(message = "Enter your city")
            @Size(min = 2, max = 80, message = "Enter your city")
            String city,

            @NotBlank(message = "Choose a district")
            @Size(max = 40)
            String district) {

        public CustomerDetails {
            name = Normalizers.trim(name);
            email = Normalizers.lower(email);
            phone = Normalizers.phone(phone);
            addressLine = Normalizers.trim(addressLine);
            city = Normalizers.trim(city);
            district = Normalizers.trim(district);
        }
    }
}
