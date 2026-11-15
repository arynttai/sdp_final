# Anuar: Factory Method and Builder

Anuar's part has two creational patterns: Factory Method and Builder. Creational
patterns help create objects. Here, Factory Method creates meals. Builder puts
those meals into an order.

## Factory Method

The app has pizza, pasta and drinks. Each type needs a different object, but the
ordering code should work with all of them in the same way.

[`MealFactory`](../src/main/java/com/restaurant/factory/creator/MealFactory.java)
is the abstract creator. Its `create()` method checks the meal category, then
calls `createMeal()`. `PizzaFactory`, `PastaFactory` and `DrinkFactory` override
this method. They create `Pizza`, `Pasta` and `Drink` objects.

All products use the
[`Meal`](../src/main/java/com/restaurant/factory/product/Meal.java) interface.
It provides methods such as `name()` and `price()`. `BaseMeal` stores the shared
meal data. `MenuDefinition` checks required values and positive prices.
[`MenuCatalog`](../src/main/java/com/restaurant/factory/catalog/MenuCatalog.java)
finds a meal by ID and selects its factory. This keeps meal creation separate
from order creation.

## Builder

An order has a customer, meals and a service type. A dine-in order also needs a
table number. Builder makes these choices easy to read.

[`Order.Builder`](../src/main/java/com/restaurant/builder/product/Order.java)
collects the details step by step. `build()` checks them and creates the `Order`.
An order needs 1 to 20 lines. Each
[`OrderLine`](../src/main/java/com/restaurant/builder/product/OrderLine.java)
allows a quantity from 1 to 10. Dine-in needs a table from 1 to 30. Takeaway must
not have a table number.

This example creates two Margheritas for table 4. The subtotal is 4000 KZT.

```java
Meal meal = new MenuCatalog().create("margherita");
Order order = new Order.Builder()
    .customer("Anuar")
    .fulfillment(Fulfillment.DINE_IN)
    .tableNumber(4)
    .addItem(new OrderLine(meal, 2))
    .build();
```

## Design choices

- **Encapsulation:** `Order` has a private constructor and no setters. It copies
  the list of lines, so later builder changes do not change that list.
- **Inheritance and polymorphism:** the three factories extend `MealFactory`.
  Each has its own `createMeal()` method. The caller uses the common type.
- **SRP:** factories create meals; the builder creates orders. Each has one job.
- **DIP:** `OrderLine` uses the `Meal` interface. It does not depend on `Pizza`.
- **DRY:** `BaseMeal` keeps shared product data and methods in one place.
- **KISS:** the code uses ordinary classes and clear builder methods.

## Working with the team

Mukhammad's Decorator adds toppings to a `Meal` before Builder receives it.
Akmaral's Strategy calculates the discount. Her Bridge creates receipts.
Mukhammad's Observer sends status updates inside the app.

The team has six patterns in six packages. `Main` is the only Java file directly
at the package root. `OrderService` connects the patterns for the website.
`Order.total()` means the price before discounts. `Pricing.total()` is the amount
to pay. See [Akmaral's notes](AKMARAL.md) and [Mukhammad's notes](MUKHAMMAD.md).

## Run the example

Use JDK 21 or newer. From the project folder in Windows PowerShell:

```powershell
.\mvnw.cmd package
java -jar target/hearth-and-table-1.0.0.jar --demo
```

On Linux or macOS, use `./mvnw package` for the build.

`Main --demo` creates pizza, pasta and drinks. It builds two orders and checks
their totals: **6800.00 KZT** for dine-in and **3400.00 KZT** for takeaway.
It throws an exception if these values are wrong. It then shows the other four
patterns working with these classes. Run the JAR without `--demo` for the website.

For the defense, use the [Factory Method UML](../output/pdf/ITALIANO-Report.pdf) and
[Builder UML](../output/pdf/ITALIANO-Report.pdf) to explain the class roles and this flow.
Anuar's original commits are
[Factory Method](https://github.com/arynttai/sdp_final/commit/cd79d48848978b503102d50f515bf99d00e9d7f3)
and [Builder](https://github.com/arynttai/sdp_final/commit/2792abab4f756389803d9d60d512ff9c47cfc61b),
both recorded under `ANAYSSA`.

Pattern explanations: [Factory Method](https://refactoring.guru/design-patterns/factory-method), [Builder](https://refactoring.guru/design-patterns/builder).
