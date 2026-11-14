package com.restaurant.observer.concrete;

import com.restaurant.observer.api.OrderObserver;
import com.restaurant.observer.event.OrderNotification;
import com.restaurant.observer.event.StatusChange;

public final class CustomerObserver implements OrderObserver {
    @Override public OrderNotification onStatusChanged(StatusChange change) {
        String message = switch (change.status()) {
            case PLACED -> "Order received.";
            case PREPARING -> "Your order is being prepared.";
            case READY -> "Your order is ready.";
            case COMPLETED -> "Order completed.";
        };
        return new OrderNotification(change.orderId(), change.status(), message, "CUSTOMER", change.occurredAt());
    }
}
