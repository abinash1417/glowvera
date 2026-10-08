package com.glowvera.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void formatsCentsAsPlainAmount() {
        assertEquals("1850.00", Money.formatAmount(185000));
        assertEquals("0.05", Money.formatAmount(5));
        assertEquals("12.30", Money.formatAmount(1230));
    }

    @Test
    void parsesAmountsBackToCents() {
        assertEquals(185000, Money.parseAmountToCents("1850.00").getAsLong());
        assertEquals(185000, Money.parseAmountToCents("1850").getAsLong());
        assertEquals(1250, Money.parseAmountToCents("12.5").getAsLong());
    }

    @Test
    void rejectsGarbageAmounts() {
        assertTrue(Money.parseAmountToCents("abc").isEmpty());
        assertTrue(Money.parseAmountToCents("-5").isEmpty());
        assertTrue(Money.parseAmountToCents("1.234").isEmpty());
        assertTrue(Money.parseAmountToCents(null).isEmpty());
    }

    @Test
    void formatsRupeesWithThousandsSeparators() {
        assertEquals("Rs. 6,950.00", Money.formatRupees(695000));
        assertEquals("Rs. 350.00", Money.formatRupees(35000));
        assertEquals("Rs. 1,234,567.89", Money.formatRupees(123456789));
    }
}
