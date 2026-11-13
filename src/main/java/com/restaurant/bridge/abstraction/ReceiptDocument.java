package com.restaurant.bridge.abstraction;

import com.restaurant.bridge.implementation.ReceiptContent;
import com.restaurant.bridge.implementation.ReceiptRenderer;
import com.restaurant.builder.product.Order;
import com.restaurant.builder.product.OrderLine;
import com.restaurant.strategy.context.Pricing;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public abstract class ReceiptDocument {
    private final ReceiptRenderer renderer;

    protected ReceiptDocument(ReceiptRenderer renderer) {
        this.renderer = Objects.requireNonNull(renderer, "Receipt renderer is required");
    }

    public final String render(Order order, Pricing pricing) {
        Objects.requireNonNull(order, "Order is required");
        Objects.requireNonNull(pricing, "Pricing is required");
        return renderer.render(content(order, pricing));
    }

    protected abstract ReceiptContent content(Order order, Pricing pricing);

    protected final List<String> details(Order order) {
        List<String> details = new ArrayList<>();
        details.add("Order: " + order.id());
        details.add("Customer: " + order.customerName());
        details.add("Service: " + order.fulfillment());
        if (order.tableNumber() != null) {
            details.add("Table: " + order.tableNumber());
        }
        if (!order.note().isBlank()) {
            details.add("Note: " + order.note());
        }
        return details;
    }

    protected final List<String> items(Order order, boolean includePrices) {
        List<String> items = new ArrayList<>();
        for (OrderLine line : order.items()) {
            String item = line.quantity() + " x " + line.meal().name();
            if (!line.toppings().isEmpty()) {
                item += " [" + String.join(", ", line.toppings()) + "]";
            }
            if (includePrices) {
                item += " = " + line.lineTotal().toPlainString() + " KZT";
            }
            items.add(item);
        }
        return items;
    }
}
