package com.restaurant.decorator.catalog;

import com.restaurant.decorator.component.ToppingType;
import com.restaurant.decorator.concrete.ExtraCheese;
import com.restaurant.decorator.concrete.Mushrooms;
import com.restaurant.decorator.concrete.Olives;
import com.restaurant.factory.product.Meal;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.UnaryOperator;

public final class ToppingCatalog {
    public record Topping(String id, String name, BigDecimal price) { }

    private final List<Topping> toppings = Arrays.stream(ToppingType.values())
        .map(type -> new Topping(type.id(), type.displayName(), type.price())).toList();
    private final Map<String, UnaryOperator<Meal>> decorators = Map.of(
        ToppingType.CHEESE.id(), ExtraCheese::new,
        ToppingType.MUSHROOMS.id(), Mushrooms::new,
        ToppingType.OLIVES.id(), Olives::new);

    public List<Topping> items() { return toppings; }

    public Meal add(Meal meal, String id) {
        if (id == null || id.isBlank() || !decorators.containsKey(id)) {
            throw new IllegalArgumentException("Unknown topping: " + id);
        }
        return decorators.get(id).apply(meal);
    }

    public String name(String id) {
        return toppings.stream().filter(topping -> topping.id().equals(id)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown topping: " + id)).name();
    }

    public Meal decorate(Meal meal, List<String> ids) {
        Objects.requireNonNull(meal, "A meal is required");
        if (ids == null || ids.size() > 3 || ids.stream().anyMatch(id -> id == null || id.isBlank())) {
            throw new IllegalArgumentException("Choose up to three valid toppings");
        }
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new IllegalArgumentException("A topping can only be added once per item");
        }
        Meal decorated = meal;
        for (String id : ids) decorated = add(decorated, id);
        return decorated;
    }
}
