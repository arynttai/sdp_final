package com.restaurant.strategy.api;

import com.restaurant.builder.product.Order;
import java.math.BigDecimal;

public interface DiscountStrategy {
    String code();

    /** Returns the discount amount in KZT, not the price after discount. */
    BigDecimal discount(Order order);
}
