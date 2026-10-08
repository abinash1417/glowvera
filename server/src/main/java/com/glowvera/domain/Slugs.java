package com.glowvera.domain;

import java.text.Normalizer;
import java.util.Locale;

public final class Slugs {

    private Slugs() {
    }

    public static String slugify(String text) {
        String slug = Normalizer.normalize(String.valueOf(text), Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (slug.length() > 150) {
            slug = slug.substring(0, 150);
        }
        slug = slug.replaceAll("-+$", "");
        return slug.isEmpty() ? "product" : slug;
    }
}
