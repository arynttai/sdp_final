package com.restaurant.builder.product;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Immutable Builder product; construction always passes through Builder.build(). */
public final class Order {
    private final UUID id;
    private final String customerName;
    private final Fulfillment fulfillment;
    private final Integer tableNumber;
    private final String note;
    private final Instant createdAt;
    private final List<OrderLine> items;
    private final BigDecimal subtotal;

    private Order(Builder builder) {
        id = UUID.randomUUID();
        customerName = builder.customerName.strip();
        fulfillment = builder.fulfillment;
        tableNumber = builder.tableNumber;
        note = builder.note.strip();
        createdAt = Instant.now();
        items = List.copyOf(builder.items);
        subtotal = items.stream().map(OrderLine::lineTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    }

    public UUID id() { return id; }
    public String customerName() { return customerName; }
    public Fulfillment fulfillment() { return fulfillment; }
    public Integer tableNumber() { return tableNumber; }
    public String note() { return note; }
    public Instant createdAt() { return createdAt; }
    public List<OrderLine> items() { return items; }
    public BigDecimal subtotal() { return subtotal; }
    /** Original amount before discounts. Use Pricing.total() for the amount payable. */
    public BigDecimal total() { return subtotal; }

    /** Builds different valid order configurations without a long public constructor. */
    public static final class Builder {
        public static final int MAX_LINES = 20;
        private String customerName;
        private Fulfillment fulfillment;
        private Integer tableNumber;
        private String note = "";
        private final List<OrderLine> items = new ArrayList<>();

        public Builder customer(String name) {
            customerName = name;
            return this;
        }

        public Builder fulfillment(Fulfillment mode) {
            fulfillment = mode;
            return this;
        }

        public Builder tableNumber(Integer table) {
            tableNumber = table;
            return this;
        }

        public Builder note(String value) {
            note = Objects.requireNonNull(value, "Note cannot be null");
            return this;
        }

        public Builder addItem(OrderLine line) {
            items.add(Objects.requireNonNull(line, "An order line is required"));
            return this;
        }

        public Order build() {
            if (customerName == null || customerName.strip().length() < 2
                    || customerName.strip().length() > 60) {
                throw new IllegalArgumentException("Customer name must contain 2 to 60 characters");
            }
            if (fulfillment == null) {
                throw new IllegalArgumentException("Choose dine-in or takeaway");
            }
            if (fulfillment == Fulfillment.DINE_IN
                    && (tableNumber == null || tableNumber < 1 || tableNumber > 30)) {
                throw new IllegalArgumentException("Dine-in orders need a table number between 1 and 30");
            }
            if (fulfillment == Fulfillment.TAKEAWAY && tableNumber != null) {
                throw new IllegalArgumentException("Takeaway orders do not have a table number");
            }
            if (items.isEmpty() || items.size() > MAX_LINES) {
                throw new IllegalArgumentException("An order must have 1 to " + MAX_LINES + " lines");
            }
            if (note.length() > 300) {
                throw new IllegalArgumentException("Note must not exceed 300 characters");
            }
            if (customerName.chars().anyMatch(Character::isISOControl)
                    || note.chars().anyMatch(c -> Character.isISOControl(c) && c != '\n' && c != '\t')) {
                throw new IllegalArgumentException("Text contains unsupported control characters");
            }
            return new Order(this);
        }
    }
}
