package com.glowvera.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.glowvera.common.Normalizers;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;


public record UpdateProductRequest(
        @Size(min = 2, max = 150, message = "Name is too short") String name,
        @Size(max = 80) String brand,
        @Size(min = 10, max = 5000, message = "Description must be at least 10 characters") String description,
        @Size(max = 3000) String ingredients,
        @Size(max = 2000) String howToUse,
        @Pattern(regexp = ProductRules.IMAGE_URL_REGEX, message = ProductRules.IMAGE_URL_MESSAGE)
        @Size(max = 500)
        String imageUrl,
        @Positive Long categoryId,
        @Size(max = 20) List<@Positive Long> skinTypeIds,
        @Size(max = 20) List<@Positive Long> concernIds,
        @JsonProperty("isActive") Boolean isActive) {

    public UpdateProductRequest {
        name = Normalizers.trim(name);
        brand = Normalizers.trim(brand);
        description = Normalizers.trim(description);
        ingredients = Normalizers.trim(ingredients);
        howToUse = Normalizers.trim(howToUse);
        imageUrl = Normalizers.trim(imageUrl);
    }

    public boolean isEmpty() {
        return name == null && brand == null && description == null && ingredients == null
                && howToUse == null && imageUrl == null && categoryId == null
                && skinTypeIds == null && concernIds == null && isActive == null;
    }
}
