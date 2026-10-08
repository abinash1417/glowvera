package com.glowvera.dto.response;

import java.time.Instant;
import java.util.List;

public final class PublicProductViews {

    private PublicProductViews() {
    }

    public record Variant(Long id, String sku, String name, long priceCents, int available) {
    }

    public record ListItem(
            Long id, String name, String slug, String brand, String imageUrl,
            Refs.PublicCategory category, List<String> skinTypes, List<String> concerns,
            long priceFromCents, boolean inStock, int variantCount, Instant createdAt) {
    }

    public record Detail(
            Long id, String name, String slug, String brand, String description,
            String ingredients, String howToUse, String imageUrl,
            Refs.PublicCategory category, List<String> skinTypes, List<String> concerns,
            List<Variant> variants, boolean inStock) {
    }
}
