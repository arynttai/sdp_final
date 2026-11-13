package com.restaurant.builder.application.dto;

import com.restaurant.factory.product.MenuDefinition;
import com.restaurant.decorator.catalog.ToppingCatalog.Topping;
import com.restaurant.observer.event.OrderStatus;
import java.util.List;

public record MenuView(List<MenuDefinition> items, List<Topping> toppings,
                       List<DiscountOption> discounts, String currency, List<OrderStatus> statuses) {
    public record DiscountOption(String id, String name, String description) { }
}
