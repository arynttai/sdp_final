package com.restaurant.bridge;

import com.restaurant.bridge.abstraction.CustomerReceipt;
import com.restaurant.bridge.abstraction.KitchenTicket;
import com.restaurant.bridge.implementation.HtmlReceiptRenderer;
import com.restaurant.bridge.implementation.ReceiptContent;
import com.restaurant.bridge.implementation.ReceiptRenderer;
import com.restaurant.bridge.implementation.TextReceiptRenderer;
import com.restaurant.builder.product.Fulfillment;
import com.restaurant.builder.product.Order;
import com.restaurant.builder.product.OrderLine;
import com.restaurant.factory.catalog.MenuCatalog;
import com.restaurant.factory.creator.PizzaFactory;
import com.restaurant.factory.product.Meal;
import com.restaurant.factory.product.MealCategory;
import com.restaurant.factory.product.MenuDefinition;
import com.restaurant.strategy.concrete.PickupDiscount;
import com.restaurant.strategy.concrete.StudentDiscount;
import com.restaurant.strategy.context.Pricing;
import com.restaurant.strategy.context.PricingCalculator;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.assertj.core.api.Assertions.*;

class ReceiptTest {
    private final MenuCatalog menu = new MenuCatalog();
    private final PricingCalculator calculator = new PricingCalculator();

    static Stream<ReceiptRenderer> renderers() {
        return Stream.of(new TextReceiptRenderer(), new HtmlReceiptRenderer());
    }

    @ParameterizedTest
    @MethodSource("renderers")
    void customerReceiptUsesTheDiscountedTotal(ReceiptRenderer renderer) {
        Order order = dineIn();
        Pricing pricing = calculator.calculate(order, new StudentDiscount());

        String receipt = new CustomerReceipt(renderer).render(order, pricing);

        assertThat(receipt).contains("Customer receipt", "Customer: Akmaral", "2 x Margherita",
            "Subtotal: 4000.00 KZT", "Discount: 400.00 KZT", "Total: 3600.00 KZT");
        assertThat(receipt).doesNotContain("Total: 4000.00 KZT");
        assertThat(order.total()).isEqualByComparingTo("4000.00");
    }

    @ParameterizedTest
    @MethodSource("renderers")
    void kitchenTicketContainsPreparationDetailsWithoutPrices(ReceiptRenderer renderer) {
        Order order = dineIn();
        Pricing pricing = calculator.calculate(order, new StudentDiscount());

        String ticket = new KitchenTicket(renderer).render(order, pricing);

        assertThat(ticket).contains("Kitchen ticket", "2 x Margherita", "Extra cheese",
            "Service: DINE_IN", "Table: 4", "Note: Serve together");
        assertThat(ticket).doesNotContain("KZT", "Subtotal:", "Discount:", "Total:");
    }

    @Test
    void takeawayReceiptsWorkWithFactoryProductsAndPickupPricing() {
        Order order = new Order.Builder().customer("Akmaral").fulfillment(Fulfillment.TAKEAWAY)
            .addItem(new OrderLine(menu.create("margherita"), 1))
            .addItem(new OrderLine(menu.create("lemonade"), 2)).build();
        Pricing pricing = calculator.calculate(order, new PickupDiscount());

        String receipt = new CustomerReceipt(new TextReceiptRenderer()).render(order, pricing);
        String ticket = new KitchenTicket(new HtmlReceiptRenderer()).render(order, pricing);

        assertThat(receipt).contains("1 x Margherita = 2000.00 KZT",
            "2 x House Lemonade = 1400.00 KZT", "Total: 2720.00 KZT");
        assertThat(ticket).contains("<section", "<li>2 x House Lemonade</li>", "Service: TAKEAWAY")
            .doesNotContain("Table:", "Note:");
    }

    @Test
    void htmlEscapesUserTextInBothDocuments() {
        Meal meal = new PizzaFactory().create(new MenuDefinition("special", "Pizza <special> & cheese",
            "Description", MealCategory.PIZZA, new BigDecimal("2000.00")));
        Order order = new Order.Builder().customer("Akmaral <script> & \"guest\"")
            .fulfillment(Fulfillment.TAKEAWAY).note("<img src=x onerror='alert(1)'> & no salt")
            .addItem(new OrderLine(meal, 1, List.of("<b>extra</b> & sauce"))).build();
        Pricing pricing = calculator.calculate(order, new StudentDiscount());

        List<String> documents = List.of(
            new CustomerReceipt(new HtmlReceiptRenderer()).render(order, pricing),
            new KitchenTicket(new HtmlReceiptRenderer()).render(order, pricing));

        for (String html : documents) {
            assertThat(html).contains("Akmaral &lt;script&gt; &amp; &quot;guest&quot;",
                "&lt;img src=x onerror=&#39;alert(1)&#39;&gt; &amp; no salt",
                "Pizza &lt;special&gt; &amp; cheese", "&lt;b&gt;extra&lt;/b&gt; &amp; sauce")
                .doesNotContain("<script>", "<img", "<special>", "<b>extra</b>");
        }
        String text = new CustomerReceipt(new TextReceiptRenderer()).render(order, pricing);
        assertThat(text).contains(order.customerName(), order.note(), meal.name());
    }

    @Test
    void htmlRendererEscapesEveryContentSection() {
        ReceiptContent content = new ReceiptContent("<title>", List.of("<detail>"),
            List.of("<item>"), List.of("<total>"));

        assertThat(new HtmlReceiptRenderer().render(content))
            .contains("<h1>&lt;title&gt;</h1>", "<p>&lt;detail&gt;</p>",
                "<li>&lt;item&gt;</li>", "<p>&lt;total&gt;</p>");
    }

    @Test
    void contentKeepsSnapshotsOfItsLists() {
        List<String> details = new ArrayList<>(List.of("Customer: Akmaral"));
        List<String> items = new ArrayList<>(List.of("1 x Margherita"));
        List<String> totals = new ArrayList<>(List.of("Total: 2000.00 KZT"));
        ReceiptContent content = new ReceiptContent("Receipt", details, items, totals);

        details.clear();
        items.clear();
        totals.clear();

        assertThat(content.details()).containsExactly("Customer: Akmaral");
        assertThat(content.items()).containsExactly("1 x Margherita");
        assertThat(content.totals()).containsExactly("Total: 2000.00 KZT");
        assertThatThrownBy(() -> content.items().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    private Order dineIn() {
        return new Order.Builder().customer("Akmaral").fulfillment(Fulfillment.DINE_IN)
            .tableNumber(4).note("Serve together")
            .addItem(new OrderLine(menu.create("margherita"), 2, List.of("Extra cheese"))).build();
    }
}
