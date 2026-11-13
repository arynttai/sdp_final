# Restaurant Ordering System

Team project in Java 21 with four implemented patterns: Anuar's **Builder** and **Factory Method**, and Akmaral's **Strategy** and **Bridge**. This is the runnable domain foundation; Observer, Decorator and the web application are still to be integrated by Mukhammad.

## Run and test

Install JDK 21 and set `JAVA_HOME`. The checked-in Maven wrapper downloads Maven automatically, with its distribution checksum verified.

```sh
./mvnw verify
java -jar target/hearth-and-table-1.0.0.jar
```

On Windows, use `mvnw.cmd verify`. `Main` creates meals, builds validated dine-in and takeaway orders, calculates discounts and renders customer/kitchen receipts in text and HTML. Prices are in KZT. GitHub Actions runs the tests and the console demonstration on pushes and pull requests.

## Anuar's two patterns

| Pattern | Role | Implementation |
| --- | --- | --- |
| Factory Method | Abstract creator | `factory.creator.MealFactory` delegates to `createMeal` |
| Factory Method | Concrete creators | `PizzaFactory`, `PastaFactory`, `DrinkFactory` |
| Factory Method | Product interface / products | `Meal`; `Pizza`, `Pasta`, `Drink` |
| Factory Method | Catalog | `MenuCatalog` selects a creator for a validated `MenuDefinition` |
| Builder | Builder | `Order.Builder` provides fluent construction and validation |
| Builder | Immutable product | `Order` holds a defensive copy of its order lines |
| Builder | Supporting values | `OrderLine`, `Fulfillment` |

The factory creates a `Meal`; an `OrderLine` refers to that interface; the builder combines lines into an order. The builder rejects missing customers, empty orders, invalid quantities and inconsistent table/fulfillment values. `BigDecimal` keeps monetary calculations exact.

```text
src/main/java/com/restaurant/
├── Main.java
├── factory/
│   ├── creator/
│   ├── product/
│   └── catalog/
├── builder/
│   └── product/
├── strategy/
│   ├── api/
│   ├── concrete/
│   └── context/
└── bridge/
    ├── abstraction/
    └── implementation/
```

`Main` is the only Java file directly at the package root. Tests follow the corresponding pattern packages. The factory has no dependency on the builder or on the remaining patterns. The builder depends on the `Meal` abstraction, allowing Decorator to wrap products later.

## Akmaral's two patterns

| Pattern | Role | Implementation |
| --- | --- | --- |
| Strategy | Strategy contract | `DiscountStrategy.code()` and `discount(Order)` |
| Strategy | Discount rules | `NoDiscount` (0%), `StudentDiscount` (10%), `PickupDiscount` (20%, takeaway only) |
| Strategy | Context / result | `PricingCalculator.calculate` returns immutable `Pricing` |
| Bridge | Document abstraction | `ReceiptDocument`, with `CustomerReceipt` and `KitchenTicket` |
| Bridge | Rendering contract | `ReceiptRenderer`, with text and HTML implementations |
| Bridge | Shared content | Immutable `ReceiptContent` keeps formatting separate from document purpose |

The calculator applies one selected discount to the order subtotal. It uses `BigDecimal` with two decimal places and `HALF_UP`, and rejects negative or excessive discounts. `Order.total()` still means the amount before discounts; use **`Pricing.total()`** for the amount payable. A 4000.00 KZT order with `STUDENT` totals 3600.00 KZT.

Both receipt types work with either renderer. Customer receipts include the discounted total; kitchen tickets include preparation details without prices. The HTML renderer escapes all dynamic text. See [Akmaral's notes](docs/AKMARAL.md) for class roles, UML, SOLID examples and the public API for integration.

## Design choices

- **SRP:** menu definitions, product creation, order construction and the console demonstration have separate responsibilities.
- **OCP / DIP:** clients use `Meal` and `MealFactory`; adding a concrete creator does not change `Order`.
- **LSP / ISP:** the three meal types share one small product contract.
- **DRY:** `BaseMeal` holds common immutable product data; money and order validation have a single implementation.
- **KISS:** ordinary Java classes, records and collections; no framework is needed to demonstrate these patterns. The Maven parent supplies dependency versions for later Spring Boot integration.

## Team integration

| Member | GitHub | Assigned patterns |
| --- | --- | --- |
| Anuar | [ANAYSSA](https://github.com/ANAYSSA) | Builder, Factory Method — implemented here |
| Akmaral | [arynttai](https://github.com/arynttai) | Strategy, Bridge — implemented here |
| Mukhammad | [vertex-blip](https://github.com/vertex-blip) | Observer, Decorator — to integrate |

Mukhammad should add `observer` and `decorator`, with role-specific subfolders, to complete the six pattern packages. Keep `Main` as the sole root class and extend its demonstration after integration. Decorator can implement `Meal` and wrap products before building the order; Observer can manage progress separately from the immutable order. Use `PricingCalculator` for totals and the Bridge documents for receipts, as described in [the integration notes](docs/AKMARAL.md#integration-for-mukhammad).

Each member must use their own GitHub-linked commit email and commit real implementation changes. Merge their commits into `main` without replacing authors. GitHub's commit history shows authors directly; profile graphs and Contributors may update later because GitHub caches them.

This branch currently covers **two creational, one behavioral and one structural pattern**. It is not yet the completed six-pattern submission. The final team submission still needs Observer, Decorator, the planned web application, screenshots, complete UML, a PDF report, source ZIP and each student's individual submission by the course deadline.

The implementation adapts Anuar's existing Builder/Factory foundation from [Final-Project-SDP](https://github.com/ANAYSSA/Final-Project-SDP), with dependencies on the other four patterns removed. It continues the [Builder](https://github.com/ANAYSSA/SDPAssignment1Builder) and [Abstract Factory assignment](https://github.com/ANAYSSA/Assignment2SDP_AbstractFactory_AnuarM); this contribution explicitly demonstrates **Factory Method**.
