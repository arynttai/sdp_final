package com.restaurant.observer.concrete;

import com.restaurant.observer.api.OrderObserver;
import com.restaurant.observer.event.OrderNotification;
import com.restaurant.observer.event.StatusChange;

public final class KitchenObserver implements OrderObserver {
    @Override public OrderNotification onStatusChanged(StatusChange change) {
        String message = switch (change.status()) {
            case PLACED -> "New order.";
            case PREPARING -> "Preparation started.";
            case READY -> "Ready for service.";
            case COMPLETED -> "Order closed.";
        };
        return new OrderNotification(change.orderId(), change.status(), message, "KITCHEN", change.occurredAt());
    }
}
