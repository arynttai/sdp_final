package com.restaurant.builder.application.dto;

import com.restaurant.builder.repository.StoredOrder;
import com.restaurant.builder.product.Fulfillment;
import com.restaurant.observer.event.OrderNotification;
import com.restaurant.observer.event.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Immutable response snapshot, created while holding the progress lock. */
public record OrderView(UUID id, String customerName, Fulfillment fulfillment, Integer tableNumber,
                        String note, OrderStatus status, Instant createdAt, List<LineView> items,
                        BigDecimal subtotal, BigDecimal discount, BigDecimal total, String currency,
                        List<OrderNotification> events) {
    public record LineView(String name, int quantity, BigDecimal unitPrice, BigDecimal lineTotal, List<String> toppings) { }
    public static OrderView of(StoredOrder stored) {
        synchronized (stored.progress()) {
            var order = stored.order();
            var pricing = stored.pricing();
            return new OrderView(order.id(), order.customerName(), order.fulfillment(), order.tableNumber(),
                order.note(), stored.progress().status(), order.createdAt(),
                order.items().stream().map(line -> new LineView(line.meal().name(), line.quantity(),
                    line.meal().price(), line.lineTotal(), line.toppings())).toList(),
                pricing.subtotal(), pricing.discount(), pricing.total(), "KZT", stored.progress().notifications());
        }
    }
}
