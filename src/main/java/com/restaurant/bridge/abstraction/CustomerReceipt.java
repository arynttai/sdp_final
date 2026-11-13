package com.restaurant.bridge.abstraction;

import com.restaurant.bridge.implementation.ReceiptContent;
import com.restaurant.bridge.implementation.ReceiptRenderer;
import com.restaurant.builder.product.Order;
import com.restaurant.strategy.context.Pricing;
import java.util.List;

public final class CustomerReceipt extends ReceiptDocument {
    public CustomerReceipt(ReceiptRenderer renderer) {
        super(renderer);
    }

    @Override
    protected ReceiptContent content(Order order, Pricing pricing) {
        return new ReceiptContent("Customer receipt", details(order), items(order, true), List.of(
            "Subtotal: " + pricing.subtotal().toPlainString() + " KZT",
            "Discount: " + pricing.discount().toPlainString() + " KZT",
            "Total: " + pricing.total().toPlainString() + " KZT"));
    }
}
