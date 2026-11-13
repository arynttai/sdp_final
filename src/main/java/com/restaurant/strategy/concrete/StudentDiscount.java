package com.restaurant.strategy.concrete;

import com.restaurant.builder.product.Order;
import com.restaurant.strategy.api.DiscountStrategy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class StudentDiscount implements DiscountStrategy {
    private static final BigDecimal RATE = new BigDecimal("0.10");

    @Override
    public String code() { return "STUDENT"; }

    @Override
    public BigDecimal discount(Order order) {
        Objects.requireNonNull(order, "An order is required");
        return order.subtotal().multiply(RATE).setScale(2, RoundingMode.HALF_UP);
    }
}
