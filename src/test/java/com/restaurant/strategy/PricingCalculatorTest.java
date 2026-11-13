package com.restaurant.strategy;

import com.restaurant.builder.product.Fulfillment;
import com.restaurant.builder.product.Order;
import com.restaurant.builder.product.OrderLine;
import com.restaurant.factory.catalog.MenuCatalog;
import com.restaurant.factory.creator.PizzaFactory;
import com.restaurant.factory.product.MealCategory;
import com.restaurant.factory.product.MenuDefinition;
import com.restaurant.strategy.api.DiscountStrategy;
import com.restaurant.strategy.concrete.NoDiscount;
import com.restaurant.strategy.concrete.PickupDiscount;
import com.restaurant.strategy.concrete.StudentDiscount;
import com.restaurant.strategy.context.Pricing;
import com.restaurant.strategy.context.PricingCalculator;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

class PricingCalculatorTest {
    private final PricingCalculator calculator = new PricingCalculator();

    @Test void strategiesPriceTheSameOrderWithoutChangingIt() {
        Order order = takeaway();

        assertPricing(calculator.calculate(order, new NoDiscount()), "4000.00", "0.00", "4000.00");
        assertPricing(calculator.calculate(order, new StudentDiscount()), "4000.00", "400.00", "3600.00");
        assertPricing(calculator.calculate(order, new PickupDiscount()), "4000.00", "800.00", "3200.00");
        assertPricing(calculator.calculate(order, new StudentDiscount()), "4000.00", "400.00", "3600.00");
        assertEquals(new BigDecimal("4000.00"), order.subtotal());
        assertEquals(order.subtotal(), order.total());
    }

    @Test void strategyCodesMatchTheIntegrationContract() {
        assertEquals("NONE", new NoDiscount().code());
        assertEquals("STUDENT", new StudentDiscount().code());
        assertEquals("PICKUP", new PickupDiscount().code());
    }

    @Test void dineInAllowsStudentDiscountButRejectsPickup() {
        Order order = new Order.Builder().customer("Akmaral").fulfillment(Fulfillment.DINE_IN)
            .tableNumber(4).addItem(new OrderLine(new MenuCatalog().create("margherita"), 2)).build();

        assertPricing(calculator.calculate(order, new NoDiscount()), "4000.00", "0.00", "4000.00");
        assertPricing(calculator.calculate(order, new StudentDiscount()), "4000.00", "400.00", "3600.00");
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
            () -> calculator.calculate(order, new PickupDiscount()));
        assertTrue(error.getMessage().contains("takeaway"));
    }

    @ParameterizedTest
    @CsvSource({"0.05, 0.01, 0.01", "0.04, 0.00, 0.01", "10.25, 1.03, 2.05"})
    void percentageDiscountsRoundHalfUp(String price, String studentAmount, String pickupAmount) {
        Order order = orderWithPrice(price);

        assertEquals(new BigDecimal(studentAmount), new StudentDiscount().discount(order));
        assertEquals(new BigDecimal(pickupAmount), new PickupDiscount().discount(order));
        Pricing pricing = calculator.calculate(order, new StudentDiscount());
        assertEquals(new BigDecimal(price).subtract(new BigDecimal(studentAmount)), pricing.total());
        assertEquals(2, pricing.total().scale());
    }

    @Test void discountIsRoundedBeforeSubtractingFromSubtotal() {
        Pricing pricing = calculator.calculate(orderWithPrice("0.05"), fixedDiscount(new BigDecimal("0.005")));

        assertPricing(pricing, "0.05", "0.01", "0.04");
    }

    @ParameterizedTest
    @ValueSource(strings = {"-0.001", "-10.00", "4000.001", "5000.00"})
    void rejectsInvalidDiscountsBeforeRounding(String amount) {
        assertThrows(IllegalArgumentException.class,
            () -> calculator.calculate(takeaway(), fixedDiscount(new BigDecimal(amount))));
    }

    @Test void aFullDiscountCanReduceTheTotalToZero() {
        assertPricing(calculator.calculate(takeaway(), fixedDiscount(new BigDecimal("4000"))),
            "4000.00", "4000.00", "0.00");
    }

    @Test void selectedStrategyIsCalledExactlyOnce() {
        Order order = takeaway();
        int[] calls = {0};
        DiscountStrategy strategy = new DiscountStrategy() {
            public String code() { return "CUSTOM"; }
            public BigDecimal discount(Order receivedOrder) {
                assertSame(order, receivedOrder);
                calls[0]++;
                return new BigDecimal("50.00");
            }
        };

        assertPricing(calculator.calculate(order, strategy), "4000.00", "50.00", "3950.00");
        assertEquals(1, calls[0]);
    }

    @Test void missingInputsOrMissingDiscountFailClearly() {
        assertThrows(NullPointerException.class, () -> calculator.calculate(null, new NoDiscount()));
        assertThrows(NullPointerException.class, () -> calculator.calculate(takeaway(), null));
        assertThrows(NullPointerException.class, () -> calculator.calculate(takeaway(), fixedDiscount(null)));
    }

    @Test void pricingRejectsInconsistentAmounts() {
        assertThrows(IllegalArgumentException.class,
            () -> new Pricing(new BigDecimal("100.00"), new BigDecimal("10.00"), new BigDecimal("100.00")));
        assertThrows(IllegalArgumentException.class,
            () -> new Pricing(new BigDecimal("100.00"), new BigDecimal("110.00"), new BigDecimal("-10.00")));
    }

    private Order takeaway() {
        return new Order.Builder().customer("Akmaral").fulfillment(Fulfillment.TAKEAWAY)
            .addItem(new OrderLine(new MenuCatalog().create("margherita"), 2)).build();
    }

    private Order orderWithPrice(String price) {
        MenuDefinition definition = new MenuDefinition("test-pizza", "Test pizza", "For rounding checks",
            MealCategory.PIZZA, new BigDecimal(price));
        return new Order.Builder().customer("Akmaral").fulfillment(Fulfillment.TAKEAWAY)
            .addItem(new OrderLine(new PizzaFactory().create(definition), 1)).build();
    }

    private DiscountStrategy fixedDiscount(BigDecimal amount) {
        return new DiscountStrategy() {
            public String code() { return "CUSTOM"; }
            public BigDecimal discount(Order order) { return amount; }
        };
    }

    private void assertPricing(Pricing pricing, String subtotal, String discount, String total) {
        assertEquals(new BigDecimal(subtotal), pricing.subtotal());
        assertEquals(new BigDecimal(discount), pricing.discount());
        assertEquals(new BigDecimal(total), pricing.total());
    }
}
