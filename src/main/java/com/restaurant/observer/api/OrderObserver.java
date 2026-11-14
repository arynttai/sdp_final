package com.restaurant.observer.api;

import com.restaurant.observer.event.OrderNotification;
import com.restaurant.observer.event.StatusChange;

/** Returns an in-app notification; no external message is sent. */
public interface OrderObserver {
    OrderNotification onStatusChanged(StatusChange change);
}
