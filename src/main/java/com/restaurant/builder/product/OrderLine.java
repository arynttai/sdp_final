package com.restaurant.builder.product;

import com.restaurant.factory.product.Meal;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record OrderLine(Meal meal, int quantity, List<String> toppings) {
    public OrderLine {
        Objects.requireNonNull(meal, "A meal is required");
        if (quantity < 1 || quantity > 10) {
            throw new IllegalArgumentException("Quantity must be between 1 and 10");
        }
        toppings = List.copyOf(Objects.requireNonNull(toppings, "Toppings cannot be null"));
    }

    public OrderLine(Meal meal, int quantity) {
        this(meal, quantity, List.of());
    }

    public BigDecimal lineTotal() {
        return meal.price().multiply(BigDecimal.valueOf(quantity));
    }
}
