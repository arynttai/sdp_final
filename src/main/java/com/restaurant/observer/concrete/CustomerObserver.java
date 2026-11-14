package com.restaurant.observer.concrete;

import com.restaurant.observer.api.OrderObserver;
import com.restaurant.observer.event.OrderNotification;
import com.restaurant.observer.event.StatusChange;

public final class CustomerObserver implements OrderObserver {
    @Override public OrderNotification onStatusChanged(StatusChange change) {
        String message = switch (change.status()) {
            case PLACED -> "Your order is in. Something good is on its way.";
            case PREPARING -> "Freshly made, just for you. Your order is being prepared.";
            case READY -> "Your order is ready. Enjoy every bite!";
            case COMPLETED -> "Thanks for joining us. See you at the table.";
        };
        return new OrderNotification(change.orderId(), change.status(), message, "CUSTOMER", change.occurredAt());
    }
}
