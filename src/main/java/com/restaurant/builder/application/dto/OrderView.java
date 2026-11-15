package com.restaurant.builder.application.dto;

import com.restaurant.builder.repository.StoredOrder;
import com.restaurant.builder.product.Fulfillment;
import com.restaurant.observer.event.OrderNotification;
import com.restaurant.observer.event.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.ArrayList;
import com.restaurant.builder.product.Order;
import com.restaurant.builder.product.OrderLine;
import com.restaurant.strategy.context.Pricing;
import java.util.UUID;

public record OrderView(UUID id, String customerName, Fulfillment fulfillment, Integer tableNumber,
                        String note, OrderStatus status, Instant createdAt, List<LineView> items,
                        BigDecimal subtotal, BigDecimal discount, BigDecimal total, String currency,
                        List<OrderNotification> events) {
    public record LineView(String name, int quantity, BigDecimal unitPrice, BigDecimal lineTotal, List<String> toppings) { }
    public static OrderView of(StoredOrder stored) {
        synchronized (stored.progress()) {
            Order order = stored.order();
            Pricing pricing = stored.pricing();
            List<LineView> lines = new ArrayList<>();
            for (OrderLine line : order.items()) {
                lines.add(new LineView(line.meal().name(), line.quantity(),
                    line.meal().price(), line.lineTotal(), line.toppings()));
            }
            return new OrderView(order.id(), order.customerName(), order.fulfillment(), order.tableNumber(),
                order.note(), stored.progress().status(), order.createdAt(),
                List.copyOf(lines),
                pricing.subtotal(), pricing.discount(), pricing.total(), "KZT", stored.progress().notifications());
        }
    }
}
