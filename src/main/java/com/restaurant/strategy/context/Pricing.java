package com.restaurant.strategy.context;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Pricing(BigDecimal subtotal, BigDecimal discount, BigDecimal total) {
    public Pricing {
        Objects.requireNonNull(subtotal, "Subtotal is required");
        Objects.requireNonNull(discount, "Discount is required");
        Objects.requireNonNull(total, "Total is required");
        if (subtotal.signum() < 0 || discount.signum() < 0
                || discount.compareTo(subtotal) > 0 || total.signum() < 0) {
            throw new IllegalArgumentException("Amounts must be non-negative and discount cannot exceed subtotal");
        }
        subtotal = subtotal.setScale(2, RoundingMode.HALF_UP);
        discount = discount.setScale(2, RoundingMode.HALF_UP);
        total = total.setScale(2, RoundingMode.HALF_UP);
        if (subtotal.subtract(discount).compareTo(total) != 0) {
            throw new IllegalArgumentException("Total must equal subtotal minus discount");
        }
    }
}
