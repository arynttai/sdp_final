package com.restaurant.observer.concrete;

import com.restaurant.observer.api.OrderObserver;
import com.restaurant.observer.event.OrderNotification;
import com.restaurant.observer.event.StatusChange;

public final class KitchenObserver implements OrderObserver {
    @Override public OrderNotification onStatusChanged(StatusChange change) {
        String message = switch (change.status()) {
            case PLACED -> "New ticket added to the kitchen queue.";
            case PREPARING -> "Preparation started for this ticket.";
            case READY -> "Ticket is ready for service or pickup.";
            case COMPLETED -> "Ticket closed.";
        };
        return new OrderNotification(change.orderId(), change.status(), message, "KITCHEN", change.occurredAt());
    }
}
