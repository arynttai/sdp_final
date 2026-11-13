package com.restaurant.strategy.context;

import com.restaurant.builder.product.Order;
import com.restaurant.strategy.api.DiscountStrategy;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class PricingCalculator {
    public Pricing calculate(Order order, DiscountStrategy strategy) {
        Objects.requireNonNull(order, "An order is required");
        Objects.requireNonNull(strategy, "A discount strategy is required");
        BigDecimal subtotal = order.subtotal();
        BigDecimal discount = Objects.requireNonNull(strategy.discount(order), "Strategy must return a discount");
        if (discount.signum() < 0 || discount.compareTo(subtotal) > 0) {
            throw new IllegalArgumentException("Discount must be between zero and the order subtotal");
        }
        discount = discount.setScale(2, RoundingMode.HALF_UP);
        return new Pricing(subtotal, discount, subtotal.subtract(discount));
    }
}
