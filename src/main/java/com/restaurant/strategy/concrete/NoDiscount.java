package com.restaurant.strategy.concrete;

import com.restaurant.builder.product.Order;
import com.restaurant.strategy.api.DiscountStrategy;
import java.math.BigDecimal;
import java.util.Objects;

public final class NoDiscount implements DiscountStrategy {
    @Override
    public String code() { return "NONE"; }

    @Override
    public BigDecimal discount(Order order) {
        Objects.requireNonNull(order, "An order is required");
        return new BigDecimal("0.00");
    }
}
