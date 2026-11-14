# Anuar: Factory Method and Builder

Anuar implemented the two creational patterns that form the ordering system's
foundation. Factory Method creates meals, and Builder assembles them into a
validated order. The original implementations remain in the shared Git history:

| Pattern | Original implementation |
| --- | --- |
| Factory Method | [cd79d48](https://github.com/arynttai/sdp_final/commit/cd79d48848978b503102d50f515bf99d00e9d7f3) |
| Builder | [2792aba](https://github.com/arynttai/sdp_final/commit/2792abab4f756389803d9d60d512ff9c47cfc61b) |

Both commits are attributed to [ANAYSSA](https://github.com/ANAYSSA) on GitHub.

## Factory Method: creating meals

[`MealFactory`](../src/main/java/com/restaurant/factory/creator/MealFactory.java)
is the abstract creator. Its public `create()` validates the menu definition and
calls the protected `createMeal()` method. `PizzaFactory`, `PastaFactory` and
`DrinkFactory` override that method to construct their corresponding products.

[`Meal`](../src/main/java/com/restaurant/factory/product/Meal.java) is the product
interface. `BaseMeal` holds shared immutable data; `Pizza`, `Pasta` and `Drink`
are the concrete products. `MenuDefinition` validates required values and
positive prices with exactly two decimal places.

[`MenuCatalog`](../src/main/java/com/restaurant/factory/catalog/MenuCatalog.java)
selects the creator by category. Callers request a menu ID and receive a `Meal`.
Product construction therefore stays outside the order builder and pricing code.
This implementation demonstrates Factory Method.

## Builder: assembling orders

[`Order.Builder`](../src/main/java/com/restaurant/builder/product/Order.java)
is a public static class nested inside `Order`. Its fluent methods collect the
customer, fulfillment mode, optional table and note, and meal lines. `build()`
validates those details before calling the private `Order` constructor.

The product copies the item list and exposes no setters. A later change to the
builder does not change an existing order. The builder permits 1–20 lines;
[`OrderLine`](../src/main/java/com/restaurant/builder/product/OrderLine.java)
validates quantities from 1 to 10. Dine-in requires a table from 1 to 30, while
takeaway must not have a table number.

```java
Meal meal = new MenuCatalog().create("margherita");
Order order = new Order.Builder()
    .customer("Anuar")
    .fulfillment(Fulfillment.DINE_IN)
    .tableNumber(4)
    .addItem(new OrderLine(meal, 2))
    .build();
```

## Team integration

`OrderService` uses these same contracts in the website. Mukhammad's Decorator
wraps a factory-created `Meal` before the builder receives it. Akmaral's Strategy
calculates a discount from the completed order; Bridge renders that order and
its pricing. Observer keeps changing status separate from the immutable order.

`Order.subtotal()` and the compatibility method `Order.total()` both mean the
amount before discounts. The payable amount comes from `Pricing.total()`.
See [Akmaral's notes](AKMARAL.md) and [Mukhammad's notes](MUKHAMMAD.md).
Class diagrams are available for [Factory Method](report/uml/factory.svg) and
[Builder](report/uml/builder.svg).

## Verification

From the repository root with JDK 21 on Windows:

```powershell
.\mvnw.cmd verify
java -jar target/hearth-and-table-1.0.0.jar --demo
```

`MealFactoryTest` checks product types, preserved menu values, fresh instances
and invalid definitions. `OrderBuilderTest` checks order configurations,
quantities, text validation, totals and immutable snapshots. `Main --demo`
demonstrates these patterns together with the other four team implementations.
