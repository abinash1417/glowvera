package com.glowvera.domain;

import java.util.OptionalLong;
import java.util.regex.Pattern;

// Money helpers. Amounts are always whole "cents" (long): 1850.00 rupees = 185000. //
public final class Money {

    private static final Pattern AMOUNT = Pattern.compile("^\\d+(\\.\\d{1,2})?$");

    private Money() {
    }

    // 185000 -> "1850.00" (the format PayHere expects). //
    public static String formatAmount(long cents) {
        long whole = cents / 100;
        long fraction = Math.abs(cents % 100);
        return whole + "." + (fraction < 10 ? "0" : "") + fraction;
    }

    public static OptionalLong parseAmountToCents(String text) {
        if (text == null || !AMOUNT.matcher(text).matches()) {
            return OptionalLong.empty();
        }
        String[] parts = text.split("\\.");
        long whole = Long.parseLong(parts[0]);
        String fraction = parts.length > 1 ? parts[1] : "";
        while (fraction.length() < 2) {
            fraction += "0";
        }
        return OptionalLong.of(whole * 100 + Long.parseLong(fraction));
    }

    public static String formatRupees(long cents) {
        String[] parts = formatAmount(cents).split("\\.");
        String whole = parts[0];
        StringBuilder grouped = new StringBuilder();
        for (int i = 0; i < whole.length(); i++) {
            if (i > 0 && (whole.length() - i) % 3 == 0) {
                grouped.append(',');
            }
            grouped.append(whole.charAt(i));
        }
        return "Rs. " + grouped + "." + parts[1];
    }
}
