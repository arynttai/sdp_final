package com.restaurant.decorator.component;

import com.restaurant.factory.product.Meal;
import com.restaurant.factory.product.MealCategory;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;

/** A topping remains a Meal, so the existing builder can consume nested wrappers. */
public abstract class ToppingDecorator implements Meal {
    private final Meal wrapped;
    private final ToppingType topping;

    protected ToppingDecorator(Meal wrapped, ToppingType topping) {
        this.wrapped = Objects.requireNonNull(wrapped, "A meal is required");
        this.topping = Objects.requireNonNull(topping, "A topping is required");
        if (wrapped.category() == MealCategory.DRINK) {
            throw new IllegalArgumentException("Toppings are available for pizza and pasta only");
        }
    }

    public final String id() { return wrapped.id(); }
    public final String name() { return wrapped.name() + " + " + topping.displayName(); }
    public final String description() {
        return wrapped.description() + ", " + topping.displayName().toLowerCase(Locale.ROOT);
    }
    public final MealCategory category() { return wrapped.category(); }
    public final BigDecimal price() { return wrapped.price().add(topping.price()); }
}
