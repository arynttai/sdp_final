package com.restaurant.observer.api;

import com.restaurant.observer.event.OrderNotification;
import com.restaurant.observer.event.StatusChange;

public interface OrderObserver {
    OrderNotification onStatusChanged(StatusChange change);
}
