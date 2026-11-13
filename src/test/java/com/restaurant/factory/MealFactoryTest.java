package com.restaurant.factory;

import com.restaurant.factory.catalog.MenuCatalog;
import com.restaurant.factory.creator.PizzaFactory;
import com.restaurant.factory.product.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class MealFactoryTest {
    private final MenuCatalog menu = new MenuCatalog();

    @Test void factoryMethodsCreateDistinctProductTypes() {
        assertThat(menu.create("margherita")).isInstanceOf(Pizza.class);
        assertThat(menu.create("alfredo")).isInstanceOf(Pasta.class);
        assertThat(menu.create("lemonade")).isInstanceOf(Drink.class);
    }

    @Test void everyCatalogEntryHasACreatorAndPreservesItsData() {
        for (MenuDefinition item : menu.items()) {
            Meal meal = menu.create(item.id());
            assertThat(meal.id()).isEqualTo(item.id());
            assertThat(meal.name()).isEqualTo(item.name());
            assertThat(meal.description()).isEqualTo(item.description());
            assertThat(meal.category()).isEqualTo(item.category());
            assertThat(meal.price()).isEqualTo(item.price());
        }
    }

    @Test void eachRequestCreatesANewProduct() {
        assertThat(menu.create("margherita")).isNotSameAs(menu.create("margherita"));
    }

    @Test void factoryRejectsADefinitionForAnotherProductFamily() {
        MenuDefinition drink = menu.items().stream().filter(i -> i.category() == MealCategory.DRINK).findFirst().orElseThrow();
        assertThatThrownBy(() -> new PizzaFactory().create(drink)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PizzaFactory().create(null)).isInstanceOf(NullPointerException.class);
    }

    @Test void unknownProductsAreRejectedAndMenuCannotBeMutated() {
        assertThatThrownBy(() -> menu.create("missing")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> menu.items().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test void menuDefinitionsValidateRequiredDataAndExactMoney() {
        assertThatThrownBy(() -> definition("", "1")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> definition("dish", "0")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> definition("dish", "-1")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> definition("dish", "1.001")).isInstanceOf(ArithmeticException.class);
        assertThat(definition("dish", "2000").price()).isEqualTo(new BigDecimal("2000.00"));
    }

    private MenuDefinition definition(String id, String price) {
        return new MenuDefinition(id, "Dish", "Description", MealCategory.PIZZA, new BigDecimal(price));
    }
}
