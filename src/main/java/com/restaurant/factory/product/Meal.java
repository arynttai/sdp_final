package com.restaurant.factory.product;

import java.math.BigDecimal;

public interface Meal {
    String id();
    String name();
    String description();
    MealCategory category();
    BigDecimal price();
}
