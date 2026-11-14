package com.restaurant.observer.event;

import java.time.Instant;
import java.util.UUID;

public record OrderNotification(UUID orderId, OrderStatus status, String message,
                                String audience, Instant occurredAt) { }
