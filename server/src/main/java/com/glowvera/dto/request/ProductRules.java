package com.glowvera.dto.request;

final class ProductRules {

    static final String IMAGE_URL_REGEX =
            "(?i)^$|^https?://\\S+$|^/images/[\\w.-]+\\.(jpe?g|png|webp|gif|avif)$";
    static final String IMAGE_URL_MESSAGE =
            "Use a full https:// link, or a file from the images folder like /images/serum.jpg";

    private ProductRules() {
    }
}
