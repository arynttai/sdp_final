package com.restaurant.decorator.catalog;

import com.restaurant.decorator.component.ToppingType;
import com.restaurant.decorator.concrete.ExtraCheese;
import com.restaurant.decorator.concrete.Mushrooms;
import com.restaurant.decorator.concrete.Olives;
import com.restaurant.factory.product.Meal;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public final class ToppingCatalog {
    public record Topping(String id, String name, BigDecimal price) { }

    private final List<Topping> toppings;

    public ToppingCatalog() {
        List<Topping> items = new ArrayList<>();
        for (ToppingType type : ToppingType.values()) {
            items.add(new Topping(type.id(), type.displayName(), type.price()));
        }
        toppings = List.copyOf(items);
    }

    public List<Topping> items() { return toppings; }

    public Meal add(Meal meal, String id) {
        if (id == null) {
            throw new IllegalArgumentException("Unknown topping: " + id);
        }
        switch (id) {
            case "cheese":
                return new ExtraCheese(meal);
            case "mushrooms":
                return new Mushrooms(meal);
            case "olives":
                return new Olives(meal);
            default:
                throw new IllegalArgumentException("Unknown topping: " + id);
        }
    }

    public String name(String id) {
        for (Topping topping : toppings) {
            if (topping.id().equals(id)) {
                return topping.name();
            }
        }
        throw new IllegalArgumentException("Unknown topping: " + id);
    }

    public Meal decorate(Meal meal, List<String> ids) {
        Objects.requireNonNull(meal, "A meal is required");
        if (ids == null || ids.size() > 3) {
            throw new IllegalArgumentException("Choose up to three valid toppings");
        }
        for (String id : ids) {
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("Choose up to three valid toppings");
            }
        }
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new IllegalArgumentException("A topping can only be added once per item");
        }
        Meal decorated = meal;
        for (String id : ids) {
            decorated = add(decorated, id);
        }
        return decorated;
    }
}
