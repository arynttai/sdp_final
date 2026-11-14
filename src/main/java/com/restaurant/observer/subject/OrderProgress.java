package com.restaurant.observer.subject;

import com.restaurant.observer.api.OrderObserver;
import com.restaurant.observer.event.OrderNotification;
import com.restaurant.observer.event.OrderStatus;
import com.restaurant.observer.event.StatusChange;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Mutable status lives beside the immutable order, with one subject per order. */
public final class OrderProgress {
    private final UUID orderId;
    private final Set<OrderObserver> observers = new LinkedHashSet<>();
    private final List<OrderNotification> notifications = new ArrayList<>();
    private OrderStatus status = OrderStatus.PLACED;
    private boolean announced;

    public OrderProgress(UUID orderId) { this.orderId = Objects.requireNonNull(orderId); }
    public synchronized void subscribe(OrderObserver observer) { observers.add(Objects.requireNonNull(observer)); }
    public synchronized void unsubscribe(OrderObserver observer) { observers.remove(observer); }
    public synchronized OrderStatus status() { return status; }
    public synchronized List<OrderNotification> notifications() { return List.copyOf(notifications); }

    public synchronized void announce() {
        if (announced) throw new IllegalStateException("Order placement was already announced");
        announced = true;
        publish();
    }

    public synchronized void advance() {
        requireAnnouncement();
        transitionTo(status.next());
    }

    public synchronized void transitionTo(OrderStatus next) {
        requireAnnouncement();
        Objects.requireNonNull(next, "A status is required");
        if (next == status) return;
        if (next != status.next()) throw new IllegalStateException("Order statuses must advance one step at a time");
        status = next;
        publish();
    }

    private void requireAnnouncement() {
        if (!announced) throw new IllegalStateException("Announce placement before changing the status");
    }

    private void publish() {
        StatusChange change = new StatusChange(orderId, status, Instant.now());
        for (OrderObserver observer : List.copyOf(observers)) {
            notifications.add(observer.onStatusChanged(change));
        }
    }
}
