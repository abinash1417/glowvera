package com.glowvera.payment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;


public final class PayHereSigner {

    private final String merchantId;
    private final String hashedSecret;

    public PayHereSigner(String merchantId, String merchantSecret) {
        this.merchantId = merchantId;
        this.hashedSecret = md5Upper(merchantSecret);
    }

    public String checkoutHash(String orderId, String amount, String currency) {
        return md5Upper(merchantId + orderId + amount + currency + hashedSecret);
    }

    public String notificationSignature(
            String notifyMerchantId, String orderId, String amount, String currency, String statusCode) {
        return md5Upper(notifyMerchantId + orderId + amount + currency + statusCode + hashedSecret);
    }

    /** Constant-time comparison, so response timing leaks nothing about the expected signature. */
    public static boolean signaturesMatch(String expected, String received) {
        if (expected == null || received == null) {
            return false;
        }
        byte[] a = expected.toUpperCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
        byte[] b = received.toUpperCase(Locale.ROOT).getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }

    static String md5Upper(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().withUpperCase().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 is not available", e);
        }
    }
}
