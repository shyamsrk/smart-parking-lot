package com.smartparkinglot.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {
    @Test
    void storesExactCents() {
        assertEquals(1250, Money.ofCents(1250).cents());
        assertEquals(new BigDecimal("12.50"), Money.ofCents(1250).asBigDecimal());
    }

    @Test
    void roundsDollarAmountsHalfUpToCents() {
        assertEquals(2, Money.ofDollars(new BigDecimal("0.015")).cents());
        assertEquals(1, Money.ofDollars(new BigDecimal("0.014")).cents());
    }

    @Test
    void rejectsNegativeAmounts() {
        assertThrows(IllegalArgumentException.class, () -> Money.ofCents(-1));
        assertThrows(
                IllegalArgumentException.class,
                () -> Money.ofDollars(new BigDecimal("-0.01")));
    }
}
