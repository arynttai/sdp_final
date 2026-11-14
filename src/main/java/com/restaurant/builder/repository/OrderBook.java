package com.restaurant.builder.repository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;

/** Each HTTP session owns one bounded book; there is no global customer store. */
public final class OrderBook {
    public static final int MAX_ORDERS = 20;
    private final Map<UUID, StoredOrder> orders = new LinkedHashMap<>();
    private final Map<UUID, StoredOrder> requests = new LinkedHashMap<>();
    public synchronized StoredOrder find(UUID id) { return orders.get(id); }
    public synchronized StoredOrder findRequest(UUID requestId) { return requests.get(requestId); }
    public synchronized List<StoredOrder> all() { return new ArrayList<>(orders.values()).reversed(); }
    public synchronized void requireCapacity() {
        if (orders.size() >= MAX_ORDERS) throw new IllegalStateException("Demo limit reached: 20 orders per session");
    }
    public synchronized void add(StoredOrder stored) {
        requireCapacity();
        if (requests.containsKey(stored.request().requestId())) throw new IllegalStateException("Request already exists");
        orders.put(stored.order().id(), stored);
        requests.put(stored.request().requestId(), stored);
    }
}
