package com.restaurant.bridge.abstraction;

import com.restaurant.bridge.implementation.ReceiptContent;
import com.restaurant.bridge.implementation.ReceiptRenderer;
import com.restaurant.builder.product.Order;
import com.restaurant.strategy.context.Pricing;
import java.util.List;

public final class KitchenTicket extends ReceiptDocument {
    public KitchenTicket(ReceiptRenderer renderer) {
        super(renderer);
    }

    @Override
    protected ReceiptContent content(Order order, Pricing pricing) {
        return new ReceiptContent("Kitchen ticket", details(order), items(order, false), List.of());
    }
}
