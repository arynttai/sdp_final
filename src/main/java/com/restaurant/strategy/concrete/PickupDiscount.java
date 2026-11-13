package com.restaurant.strategy.concrete;

import com.restaurant.builder.product.Fulfillment;
import com.restaurant.builder.product.Order;
import com.restaurant.strategy.api.DiscountStrategy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class PickupDiscount implements DiscountStrategy {
    private static final BigDecimal RATE = new BigDecimal("0.20");

    @Override
    public String code() { return "PICKUP"; }

    @Override
    public BigDecimal discount(Order order) {
        Objects.requireNonNull(order, "An order is required");
        if (order.fulfillment() != Fulfillment.TAKEAWAY) {
            throw new IllegalArgumentException("PICKUP discount is only available for takeaway orders");
        }
        return order.subtotal().multiply(RATE).setScale(2, RoundingMode.HALF_UP);
    }
}
