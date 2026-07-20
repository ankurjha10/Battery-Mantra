package com.api.batterymantra.util;

import java.math.BigDecimal;

public class PricingUtility {

    /**
     * Applies standard retail rounding to a price.
     * Logic: 
     * - If last two digits of integer value are <= 50, replace with 49.
     * - If last two digits of integer value are > 50, replace with 99.
     * Example: 19779 -> 19799, 17715 -> 17749
     */
    public static BigDecimal applyRetailRounding(BigDecimal value) {
        if (value == null) return null;
        
        long intVal = value.longValue();
        long firstPart = intVal / 100;
        long lastTwoDigits = intVal % 100;
        
        long newLastTwo;
        if (lastTwoDigits <= 50) {
            newLastTwo = 49;
        } else {
            newLastTwo = 99;
        }
        
        long roundedInt = firstPart * 100 + newLastTwo;
        return BigDecimal.valueOf(roundedInt);
    }
}
