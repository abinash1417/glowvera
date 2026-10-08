package com.glowvera.common;

import java.util.Locale;

public final class Normalizers {

    public static final String PHONE_REGEX = "^07\\d{8}$";
    public static final String PHONE_MESSAGE = "Enter a valid mobile number, e.g. 0771234567";

    private Normalizers() {
    }

    public static String trim(String s) {
        return s == null ? null : s.trim();
    }

    public static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    public static String lower(String s) {
        return s == null ? null : s.trim().toLowerCase(Locale.ROOT);
    }

    public static String upper(String s) {
        return s == null ? null : s.trim().toUpperCase(Locale.ROOT);
    }

    public static String phone(String s) {
        String t = trimToNull(s);
        if (t == null) {
            return null;
        }
        t = t.replaceAll("[\\s-]", "");
        return t.startsWith("+94") ? "0" + t.substring(3) : t;
    }
}
