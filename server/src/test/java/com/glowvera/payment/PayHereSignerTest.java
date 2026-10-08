package com.glowvera.payment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PayHereSignerTest {

    private final PayHereSigner signer = new PayHereSigner("1211149", "secret-value");

    @Test
    void md5IsUppercaseHex() {
        // well-known MD5 test vector
        assertEquals("5D41402ABC4B2A76B9719D911017C592", PayHereSigner.md5Upper("hello"));
    }

    @Test
    void checkoutHashFollowsThePayHereFormula() {
        String expected = PayHereSigner.md5Upper(
                "1211149" + "GLW-2026-00007" + "6950.00" + "LKR" + PayHereSigner.md5Upper("secret-value"));
        assertEquals(expected, signer.checkoutHash("GLW-2026-00007", "6950.00", "LKR"));
    }

    @Test
    void notificationSignatureCoversTheStatusCode() {
        String success = signer.notificationSignature("1211149", "GLW-2026-00007", "6950.00", "LKR", "2");
        String failed = signer.notificationSignature("1211149", "GLW-2026-00007", "6950.00", "LKR", "-2");
        assertFalse(success.equals(failed));
    }

    @Test
    void signatureComparisonIgnoresCaseButNotContent() {
        String sig = signer.checkoutHash("A", "1.00", "LKR");
        assertTrue(PayHereSigner.signaturesMatch(sig, sig.toLowerCase()));
        assertFalse(PayHereSigner.signaturesMatch(sig, "0000"));
        assertFalse(PayHereSigner.signaturesMatch(sig, null));
    }
}
