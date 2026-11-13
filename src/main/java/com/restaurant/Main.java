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

/** Demonstrates the team's patterns using the same menu and orders. */
public final class Main {
    private Main() { }

    public static void main(String[] args) {
        MenuCatalog menu = new MenuCatalog();
        Meal pizza = menu.create("margherita");
        Meal pasta = menu.create("alfredo");
        Meal drink = menu.create("lemonade");

        System.out.println("RESTAURANT ORDERING SYSTEM — Anuar: Factory Method + Builder");
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
        System.out.println("PASS — Factory Method products integrated with validated immutable Builder orders.");

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
