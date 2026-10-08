package com.glowvera.domain;

import java.util.UUID;
import java.util.regex.Pattern;

public final class OrderCodes {

    public static final String PATTERN = "^GLW-\\d{4}-\\d{5,}$";
    private static final Pattern COMPILED = Pattern.compile(PATTERN);

    private OrderCodes() {
    }

    public static String temporary() {
        return "TMP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }

    public static String format(long id, int year) {
        return String.format("GLW-%d-%05d", year, id);
    }

    public static boolean isValid(String code) {
        return code != null && COMPILED.matcher(code).matches();
    }
}
