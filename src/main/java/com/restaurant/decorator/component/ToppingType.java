package com.restaurant.decorator.component;

import java.math.BigDecimal;

public enum ToppingType {
    CHEESE("cheese", "Extra cheese", "350.00"),
    MUSHROOMS("mushrooms", "Mushrooms", "300.00"),
    OLIVES("olives", "Olives", "300.00");

    private final String id;
    private final String displayName;
    private final BigDecimal price;

    ToppingType(String id, String displayName, String price) {
        this.id = id;
        this.displayName = displayName;
        this.price = new BigDecimal(price);
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public BigDecimal price() { return price; }
}
