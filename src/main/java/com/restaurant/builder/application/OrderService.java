package com.restaurant.builder.application;

import com.restaurant.builder.application.dto.*;
import com.restaurant.builder.product.*;
import com.restaurant.builder.repository.*;
import com.restaurant.bridge.abstraction.*;
import com.restaurant.bridge.implementation.*;
import com.restaurant.decorator.catalog.ToppingCatalog;
import com.restaurant.factory.catalog.MenuCatalog;
import com.restaurant.factory.product.Meal;
import com.restaurant.observer.concrete.*;
import com.restaurant.observer.event.OrderStatus;
import com.restaurant.observer.subject.OrderProgress;
import com.restaurant.strategy.api.DiscountStrategy;
import com.restaurant.strategy.concrete.*;
import com.restaurant.strategy.context.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public final class OrderService {
    private final MenuCatalog menu = new MenuCatalog();
    private final ToppingCatalog toppings = new ToppingCatalog();
    private final PricingCalculator calculator = new PricingCalculator();
    private final Map<String, DiscountStrategy> discounts = Map.of(
        "NONE", new NoDiscount(), "STUDENT", new StudentDiscount(), "PICKUP", new PickupDiscount());

    public MenuView menu() {
        return new MenuView(menu.items(), toppings.items(), List.of(
            new MenuView.DiscountOption("NONE", "No discount", "Regular menu prices"),
            new MenuView.DiscountOption("STUDENT", "Student · 10%", "Student discount"),
            new MenuView.DiscountOption("PICKUP", "Takeaway · 20%", "Available for takeaway orders")),
            "KZT", List.of(OrderStatus.values()));
    }

    public PriceView quote(QuoteRequest request) {
        if (request == null) throw new IllegalArgumentException("Order details are required");
        Order order = build(request.items(), request.fulfillment(), "Quote guest",
            request.fulfillment() == Fulfillment.DINE_IN ? 1 : null, "");
        return PriceView.of(calculator.calculate(order, discount(request.discountCode())));
    }

    public OrderView place(OrderRequest request, OrderBook book) {
        if (request == null || request.requestId() == null) throw new IllegalArgumentException("A requestId UUID is required");
        synchronized (book) {
            StoredOrder previous = book.findRequest(request.requestId());
            if (previous != null) {
                if (!previous.request().equals(request)) throw new IllegalStateException("This requestId was already used for a different order");
                return OrderView.of(previous);
            }
            book.requireCapacity();
            Order order = build(request.items(), request.fulfillment(), request.customerName(), request.tableNumber(), request.note());
            Pricing pricing = calculator.calculate(order, discount(request.discountCode()));
            OrderProgress progress = new OrderProgress(order.id());
            progress.subscribe(new CustomerObserver());
            progress.subscribe(new KitchenObserver());
            progress.announce();
            StoredOrder stored = new StoredOrder(order, pricing, progress, request);
            book.add(stored);
            return OrderView.of(stored);
        }
    }

    public List<OrderView> list(OrderBook book) {
        List<OrderView> orders = new ArrayList<>();
        for (StoredOrder stored : book.all()) {
            orders.add(OrderView.of(stored));
        }
        return List.copyOf(orders);
    }
    public OrderView get(UUID id, OrderBook book) { return OrderView.of(find(id, book)); }
    public OrderView advance(UUID id, OrderBook book) {
        StoredOrder stored = find(id, book);
        synchronized (stored.progress()) {
            stored.progress().advance();
            return OrderView.of(stored);
        }
    }
    public OrderView transition(UUID id, OrderStatus status, OrderBook book) {
        StoredOrder stored = find(id, book);
        synchronized (stored.progress()) {
            stored.progress().transitionTo(status);
            return OrderView.of(stored);
        }
    }
    public record Receipt(String body, String contentType) { }
    public Receipt receipt(UUID id, String format, String type, OrderBook book) {
        return receipt(id, format, type, book, "dark");
    }
    public Receipt receipt(UUID id, String format, String type, OrderBook book, String theme) {
        if (!"dark".equals(theme) && !"light".equals(theme)) {
            throw new IllegalArgumentException("Receipt theme must be dark or light");
        }
        StoredOrder stored = find(id, book);
        ReceiptRenderer renderer = switch (format) {
            case "text" -> new TextReceiptRenderer();
            case "html" -> new HtmlReceiptRenderer();
            default -> throw new IllegalArgumentException("Receipt format must be text or html");
        };
        ReceiptDocument document = switch (type) {
            case "customer" -> new CustomerReceipt(renderer);
            case "kitchen" -> new KitchenTicket(renderer);
            default -> throw new IllegalArgumentException("Receipt type must be customer or kitchen");
        };
        String body = document.render(stored.order(), stored.pricing());
        if (format.equals("html")) {
            String palette = theme.equals("light") ? "color-scheme:light;background:#f7f2e9;color:#241e18"
                : "color-scheme:dark;background:#171410;color:#f4ede3";
            body = "<!doctype html><html lang=\"en\"><head><meta charset=\"utf-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><title>ITALIANO — Receipt</title><style>html{"
                + palette + "}body{max-width:700px;margin:3rem auto;padding:0 24px;font:15px/1.6 system-ui}h1{font-size:26px;font-weight:600;margin-bottom:24px}li{margin:.6rem 0}p{overflow-wrap:anywhere}@media print{html{color-scheme:light;background:white;color:black}body{margin:0}}</style></head><body>"
                + body + "</body></html>";
        }
        return new Receipt(body, format.equals("html") ? "text/html;charset=UTF-8" : "text/plain;charset=UTF-8");
    }

    private DiscountStrategy discount(String code) {
        if (code == null || !discounts.containsKey(code)) throw new IllegalArgumentException("Choose a valid discount");
        return discounts.get(code);
    }
    private StoredOrder find(UUID id, OrderBook book) {
        StoredOrder stored = book.find(id);
        if (stored == null) throw new OrderNotFoundException();
        return stored;
    }
    private Order build(List<ItemRequest> items, Fulfillment fulfillment, String customer, Integer table, String note) {
        if (items == null || items.isEmpty() || items.size() > Order.Builder.MAX_LINES)
            throw new IllegalArgumentException("Choose between 1 and 20 menu items");
        Order.Builder builder = new Order.Builder().customer(customer).fulfillment(fulfillment).tableNumber(table).note(note == null ? "" : note);
        for (ItemRequest item : items) {
            if (item == null || item.menuItemId() == null) throw new IllegalArgumentException("Choose a menu item");
            Meal meal = toppings.decorate(menu.create(item.menuItemId()), item.toppings());
            List<String> toppingNames = new ArrayList<>();
            for (String toppingId : item.toppings()) {
                toppingNames.add(toppings.name(toppingId));
            }
            builder.addItem(new OrderLine(meal, item.quantity(), toppingNames));
        }
        return builder.build();
    }
}
