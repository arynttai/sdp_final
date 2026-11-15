package com.restaurant.strategy.api;

import com.restaurant.builder.product.Order;
import java.math.BigDecimal;

public interface DiscountStrategy {
    String code();

    BigDecimal discount(Order order);
}
