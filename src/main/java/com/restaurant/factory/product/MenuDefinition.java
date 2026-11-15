package com.restaurant.factory.product;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record MenuDefinition(String id, String name, String description, MealCategory category, BigDecimal price) {
    public MenuDefinition {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("Menu id is required");
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Meal name is required");
        Objects.requireNonNull(description, "Description is required");
        Objects.requireNonNull(category, "Meal category is required");
        Objects.requireNonNull(price, "Price is required");
        if (price.signum() <= 0) throw new IllegalArgumentException("Price must be positive");
        price = price.setScale(2, RoundingMode.UNNECESSARY);
    }
}
