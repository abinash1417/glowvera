package com.glowvera.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.glowvera.common.Normalizers;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateProductRequest(
        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 150, message = "Name is too short")
        String name,

        @Size(max = 80) String brand,

        @NotBlank(message = "Description is required")
        @Size(min = 10, max = 5000, message = "Description must be at least 10 characters")
        String description,

        @Size(max = 3000) String ingredients,
        @Size(max = 2000) String howToUse,

        @Pattern(regexp = ProductRules.IMAGE_URL_REGEX, message = ProductRules.IMAGE_URL_MESSAGE)
        @Size(max = 500)
        String imageUrl,

        @NotNull(message = "Choose a category") @Positive Long categoryId,
        @Size(max = 20) List<@Positive Long> skinTypeIds,
        @Size(max = 20) List<@Positive Long> concernIds,
        @JsonProperty("isActive") Boolean isActive) {

    public CreateProductRequest {
        name = Normalizers.trim(name);
        brand = Normalizers.trimToNull(brand);
        description = Normalizers.trim(description);
        ingredients = Normalizers.trimToNull(ingredients);
        howToUse = Normalizers.trimToNull(howToUse);
        imageUrl = Normalizers.trimToNull(imageUrl);
    }
}
