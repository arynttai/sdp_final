package com.restaurant;

import com.restaurant.builder.product.Fulfillment;
import com.restaurant.builder.product.Order;
import com.restaurant.builder.product.OrderLine;
import com.restaurant.factory.catalog.MenuCatalog;
import com.restaurant.factory.product.Meal;
import java.math.BigDecimal;

/** Anuar's integrated Factory Method + Builder demonstration. */
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
        System.out.println("  Total: " + order.total() + " KZT");
    }
}
