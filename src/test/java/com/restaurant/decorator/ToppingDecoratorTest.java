package com.restaurant.decorator;

import com.restaurant.builder.product.Fulfillment;
import com.restaurant.builder.product.Order;
import com.restaurant.builder.product.OrderLine;
import com.restaurant.decorator.catalog.ToppingCatalog;
import com.restaurant.decorator.concrete.ExtraCheese;
import com.restaurant.decorator.concrete.Mushrooms;
import com.restaurant.decorator.concrete.Olives;
import com.restaurant.factory.catalog.MenuCatalog;
import com.restaurant.factory.product.Meal;
import com.restaurant.strategy.concrete.StudentDiscount;
import com.restaurant.strategy.context.PricingCalculator;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class ToppingDecoratorTest {
    private final MenuCatalog menu = new MenuCatalog();
    private final ToppingCatalog toppings = new ToppingCatalog();

    @Test void wrappersComposeWithoutChangingTheBaseProduct() {
        Meal base = menu.create("margherita");
        Meal decorated = new Olives(new Mushrooms(new ExtraCheese(base)));
        assertThat(decorated.price()).isEqualByComparingTo("2950.00");
        assertThat(decorated.name()).isEqualTo("Margherita + Extra cheese + Mushrooms + Olives");
        assertThat(decorated.description()).contains("extra cheese", "mushrooms", "olives");
        assertThat(decorated.id()).isEqualTo(base.id());
        assertThat(decorated.category()).isEqualTo(base.category());
        assertThat(base.name()).isEqualTo("Margherita");
        assertThat(base.price()).isEqualByComparingTo("2000.00");
    }

    @Test void wrappingOrderPreservesTheSamePriceAndMealContract() {
        Meal base = menu.create("alfredo");
        for (Meal decorated : List.of(new ExtraCheese(new Mushrooms(base)), new Mushrooms(new ExtraCheese(base)))) {
            assertThat(decorated.price()).isEqualByComparingTo("3450.00");
            assertThat(decorated.name()).contains("Extra cheese", "Mushrooms");
            assertThat(decorated.id()).isEqualTo(base.id());
            assertThat(decorated.category()).isEqualTo(base.category());
        }
    }

    @Test void declaredMenuPricesMatchActualDecoratorAdditions() {
        Meal base = menu.create("pesto");
        assertThat(toppings.items()).hasSize(3);
        for (var topping : toppings.items()) {
            Meal decorated = toppings.add(base, topping.id());
            assertThat(decorated.price().subtract(base.price())).isEqualByComparingTo(topping.price());
            assertThat(decorated.name()).contains(topping.name());
            assertThat(toppings.name(topping.id())).isEqualTo(topping.name());
        }
        assertThatThrownBy(() -> toppings.items().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void drinksRejectEveryToppingWhileAnUndecoratedDrinkRemainsValid() {
        Meal drink = menu.create("lemonade");
        for (var topping : toppings.items()) {
            assertThatThrownBy(() -> toppings.add(drink, topping.id()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Toppings are available for pizza and pasta only");
        }
        assertThat(toppings.decorate(drink, List.of())).isSameAs(drink);
    }

    @Test void toppingSelectionsRejectDuplicatesUnknownIdsAndMalformedValues() {
        Meal base = menu.create("margherita");
        assertThatThrownBy(() -> toppings.decorate(base, List.of("cheese", "cheese")))
            .isInstanceOf(IllegalArgumentException.class).hasMessage("A topping can only be added once per item");
        assertThatThrownBy(() -> toppings.decorate(base, List.of("cheese", "mushrooms", "olives", "cheese")))
            .isInstanceOf(IllegalArgumentException.class).hasMessage("Choose up to three valid toppings");
        assertThatThrownBy(() -> toppings.decorate(base, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> toppings.decorate(base, Arrays.asList("cheese", null))).isInstanceOf(IllegalArgumentException.class);
        for (String id : List.of("", " ", "missing", "CHEESE")) {
            assertThatThrownBy(() -> toppings.decorate(base, List.of(id))).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> toppings.add(base, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> toppings.name("missing")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void builderAndExistingStrategyUseTheDecoratedPriceForEveryUnit() {
        Meal decorated = new ExtraCheese(menu.create("margherita"));
        Order order = new Order.Builder().customer("Mukhammad").fulfillment(Fulfillment.TAKEAWAY)
            .addItem(new OrderLine(decorated, 2, List.of("Extra cheese"))).build();
        var pricing = new PricingCalculator().calculate(order, new StudentDiscount());
        assertThat(decorated.price()).isEqualByComparingTo("2350.00");
        assertThat(order.subtotal()).isEqualByComparingTo("4700.00");
        assertThat(pricing.subtotal()).isEqualByComparingTo("4700.00");
        assertThat(pricing.discount()).isEqualByComparingTo("470.00");
        assertThat(pricing.total()).isEqualByComparingTo("4230.00");
        assertThat(order.total()).isEqualByComparingTo("4700.00");
    }
}
