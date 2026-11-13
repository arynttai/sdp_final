package com.restaurant.bridge.implementation;

import java.util.List;
import java.util.Objects;

/** Plain receipt data, shared by every output format. */
public record ReceiptContent(String title, List<String> details, List<String> items, List<String> totals) {
    public ReceiptContent {
        Objects.requireNonNull(title, "Receipt title is required");
        details = List.copyOf(details);
        items = List.copyOf(items);
        totals = List.copyOf(totals);
    }
}
