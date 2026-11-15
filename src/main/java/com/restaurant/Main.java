package com.restaurant;

import com.restaurant.bridge.abstraction.CustomerReceipt;
import com.restaurant.bridge.abstraction.KitchenTicket;
import com.restaurant.bridge.implementation.HtmlReceiptRenderer;
import com.restaurant.bridge.implementation.ReceiptRenderer;
import com.restaurant.bridge.implementation.TextReceiptRenderer;
import com.restaurant.builder.product.Fulfillment;
import com.restaurant.builder.product.Order;
import com.restaurant.builder.product.OrderLine;
import com.restaurant.factory.catalog.MenuCatalog;
import com.restaurant.factory.product.Meal;
import com.restaurant.strategy.api.DiscountStrategy;
import com.restaurant.strategy.concrete.NoDiscount;
import com.restaurant.strategy.concrete.PickupDiscount;
import com.restaurant.strategy.concrete.StudentDiscount;
import com.restaurant.strategy.context.Pricing;
import com.restaurant.strategy.context.PricingCalculator;
import java.math.BigDecimal;
import com.restaurant.decorator.concrete.ExtraCheese;
import com.restaurant.observer.concrete.CustomerObserver;
import com.restaurant.observer.concrete.KitchenObserver;
import com.restaurant.observer.subject.OrderProgress;
import com.restaurant.observer.event.OrderNotification;
import com.restaurant.observer.event.OrderStatus;
import java.util.Arrays;
import java.util.List;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(proxyBeanMethods = false)
public class Main {
    public static void main(String[] args) {
        if (Arrays.asList(args).contains("--demo")) {
            runBasicDemo();
            runIntegratedDemo();
        } else {
            SpringApplication.run(Main.class, args);
        }
    }

    private static void runBasicDemo() {
        MenuCatalog menu = new MenuCatalog();
        Meal pizza = menu.create("margherita");
        Meal pasta = menu.create("alfredo");
        Meal drink = menu.create("lemonade");

        System.out.println("RESTAURANT ORDERING SYSTEM: Anuar: Factory Method + Builder");
        for (Meal meal : new Meal[]{pizza, pasta, drink}) {
            System.out.printf("Factory created %s: %s (%s KZT)%n",
                meal.getClass().getSimpleName(), meal.name(), meal.price());
        }

        Order dineIn = new Order.Builder()
            .customer("Anuar")
            .fulfillment(Fulfillment.DINE_IN)
            .tableNumber(4)
            .note("Please serve the pasta first")
            .addItem(new OrderLine(pizza, 2))
            .addItem(new OrderLine(pasta, 1))
            .build();

        Order takeaway = new Order.Builder()
            .customer("Guest")
            .fulfillment(Fulfillment.TAKEAWAY)
            .addItem(new OrderLine(pizza, 1))
            .addItem(new OrderLine(drink, 2))
            .build();

        printOrder(dineIn);
        printOrder(takeaway);
        if (dineIn.total().compareTo(new BigDecimal("6800.00")) != 0
                || takeaway.total().compareTo(new BigDecimal("3400.00")) != 0) {
            throw new IllegalStateException("Unexpected integrated order totals");
        }
        System.out.println("PASS: Factory Method products integrated with validated immutable Builder orders.");

        PricingCalculator calculator = new PricingCalculator();
        System.out.println("\nAkmaral: Strategy discounts for the takeaway order");
        for (DiscountStrategy strategy : new DiscountStrategy[]{new NoDiscount(), new StudentDiscount(), new PickupDiscount()}) {
            Pricing pricing = calculator.calculate(takeaway, strategy);
            System.out.printf("  %s: subtotal %s, discount %s, total %s KZT%n",
                strategy.code(), pricing.subtotal(), pricing.discount(), pricing.total());
        }

        Pricing studentPricing = calculator.calculate(dineIn, new StudentDiscount());
        System.out.println("\nAkmaral: Bridge receipts for the dine-in order with STUDENT discount");
        printReceipts(dineIn, studentPricing);
    }

    private static void runIntegratedDemo() {
        System.out.println("\n1. Factory Method: create a meal");
        MenuCatalog menu = new MenuCatalog();
        Meal pizza = menu.create("margherita");
        System.out.println(pizza.name() + ": " + pizza.price() + " KZT");

        System.out.println("\n2. Decorator: add cheese");
        Meal withCheese = new ExtraCheese(pizza);
        System.out.println(withCheese.name() + ": " + withCheese.price() + " KZT");

        System.out.println("\n3. Builder: create an order");
        Order order = new Order.Builder()
            .customer("Demo guest")
            .fulfillment(Fulfillment.TAKEAWAY)
            .note("Please slice for sharing")
            .addItem(new OrderLine(withCheese, 2, List.of("Extra cheese")))
            .build();
        printOrder(order);

        System.out.println("\n4. Strategy: apply the student discount");
        PricingCalculator calculator = new PricingCalculator();
        Pricing pricing = calculator.calculate(order, new StudentDiscount());
        System.out.println("Discount: " + pricing.discount() + " KZT");
        System.out.println("Total: " + pricing.total() + " KZT");
        if (order.subtotal().compareTo(new BigDecimal("4700.00")) != 0
                || pricing.total().compareTo(new BigDecimal("4230.00")) != 0) {
            throw new IllegalStateException("Wrong order total");
        }

        System.out.println("\n5. Observer: update the order status");
        OrderProgress progress = new OrderProgress(order.id());
        progress.subscribe(new CustomerObserver());
        progress.subscribe(new KitchenObserver());
        progress.announce();
        progress.advance();
        progress.advance();
        progress.advance();
        for (OrderNotification notification : progress.notifications()) {
            System.out.println(notification.audience() + " / " + notification.status()
                + ": " + notification.message());
        }
        if (progress.status() != OrderStatus.COMPLETED || progress.notifications().size() != 8) {
            throw new IllegalStateException("Wrong status or notification count");
        }

        System.out.println("\n6. Bridge: print the customer receipt as text");
        CustomerReceipt receipt = new CustomerReceipt(new TextReceiptRenderer());
        System.out.println(receipt.render(order, pricing));
        System.out.println("PASS: all six patterns work together.");
    }

    private static void printReceipts(Order order, Pricing pricing) {
        for (ReceiptRenderer renderer : new ReceiptRenderer[]{new TextReceiptRenderer(), new HtmlReceiptRenderer()}) {
            System.out.println(new CustomerReceipt(renderer).render(order, pricing));
            System.out.println(new KitchenTicket(renderer).render(order, pricing));
        }
    }

    private static void printOrder(Order order) {
        System.out.printf("%n%s for %s%s%n", order.fulfillment(), order.customerName(),
            order.tableNumber() == null ? "" : " at table " + order.tableNumber());
        for (OrderLine line : order.items()) {
            System.out.printf("  %d x %s = %s KZT%n", line.quantity(), line.meal().name(), line.lineTotal());
        }
        if (!order.note().isBlank()) {
            System.out.println("  Note: " + order.note());
        }
        System.out.println("  Subtotal before discount: " + order.subtotal() + " KZT");
    }
}
