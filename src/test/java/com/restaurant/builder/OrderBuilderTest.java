package com.restaurant.builder;

import com.restaurant.builder.product.Fulfillment;
import com.restaurant.builder.product.Order;
import com.restaurant.builder.product.OrderLine;
import com.restaurant.factory.catalog.MenuCatalog;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrderBuilderTest {
    private final MenuCatalog menu = new MenuCatalog();

    private OrderLine line() {
        return new OrderLine(menu.create("margherita"), 2);
    }

    private Order.Builder takeaway() {
        return new Order.Builder().customer("Anuar").fulfillment(Fulfillment.TAKEAWAY);
    }

    @Test void productSnapshotsBuilderState() {
        Order.Builder builder = takeaway().customer("  Anuar  ").addItem(line());
        Order first = builder.build();
        builder.customer("Another guest").addItem(new OrderLine(menu.create("lemonade"), 1));
        Order second = builder.build();

        assertEquals("Anuar", first.customerName());
        assertEquals(1, first.items().size());
        assertEquals(new BigDecimal("4000.00"), first.total());
        assertEquals(new BigDecimal("4700.00"), second.total());
        assertNotEquals(first.id(), second.id());
        assertNotNull(first.createdAt());
        assertThrows(UnsupportedOperationException.class, () -> first.items().add(line()));
    }

    @Test void factoryProductsCombineIntoADineInOrder() {
        Order order = new Order.Builder().customer("Guest").fulfillment(Fulfillment.DINE_IN)
            .tableNumber(3).note("  Serve together  ")
            .addItem(new OrderLine(menu.create("margherita"), 2))
            .addItem(new OrderLine(menu.create("alfredo"), 1))
            .addItem(new OrderLine(menu.create("lemonade"), 3)).build();

        assertEquals(new BigDecimal("8900.00"), order.subtotal());
        assertEquals(order.subtotal(), order.total());
        assertEquals(3, order.tableNumber());
        assertEquals("Serve together", order.note());
    }

    @Test void builderRejectsIncompleteOrInconsistentOrders() {
        assertThrows(IllegalArgumentException.class, () -> new Order.Builder().build());
        assertThrows(IllegalArgumentException.class, () -> takeaway().build());
        assertThrows(IllegalArgumentException.class,
            () -> new Order.Builder().customer("Anuar").addItem(line()).build());
        assertThrows(IllegalArgumentException.class,
            () -> takeaway().fulfillment(Fulfillment.DINE_IN).addItem(line()).build());
        assertThrows(IllegalArgumentException.class,
            () -> takeaway().tableNumber(2).addItem(line()).build());
        for (int table : new int[]{0, -1, 31, Integer.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class,
                () -> takeaway().fulfillment(Fulfillment.DINE_IN).tableNumber(table).addItem(line()).build());
        }
    }

    @Test void linesAndQuantitiesHaveExplicitLimits() {
        for (int quantity : new int[]{Integer.MIN_VALUE, -1, 0, 11, Integer.MAX_VALUE}) {
            assertThrows(IllegalArgumentException.class,
                () -> new OrderLine(menu.create("margherita"), quantity));
        }
        assertEquals(new BigDecimal("20000.00"), new OrderLine(menu.create("margherita"), 10).lineTotal());
        Order.Builder builder = takeaway();
        for (int i = 0; i < Order.Builder.MAX_LINES; i++) builder.addItem(line());
        assertEquals(20, builder.build().items().size());
        builder.addItem(line());
        assertThrows(IllegalArgumentException.class, builder::build);
    }

    @Test void userTextIsValidatedAndOptionalNoteDefaultsToEmpty() {
        for (String name : new String[]{"", " ", "A", "A".repeat(61), "An\nuar"}) {
            assertThrows(IllegalArgumentException.class, () -> takeaway().customer(name).addItem(line()).build());
        }
        assertThrows(IllegalArgumentException.class, () -> takeaway().note("x".repeat(301)).addItem(line()).build());
        assertThrows(IllegalArgumentException.class, () -> takeaway().note("bad\u0000note").addItem(line()).build());
        assertEquals("", takeaway().addItem(line()).build().note());
        assertEquals("Line one\nLine two", takeaway().note("Line one\nLine two").addItem(line()).build().note());
    }

    @Test void toppingMetadataIsDefensivelyCopiedForFutureDecoratorIntegration() {
        List<String> labels = new ArrayList<>(List.of("Extra cheese"));
        OrderLine line = new OrderLine(menu.create("margherita"), 1, labels);
        labels.add("Olives");
        assertEquals(List.of("Extra cheese"), line.toppings());
        assertThrows(UnsupportedOperationException.class, () -> line.toppings().add("Herbs"));
        assertThrows(NullPointerException.class, () -> takeaway().addItem(null));
    }
}
