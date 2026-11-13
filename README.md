# Restaurant Ordering System

Anuar's contribution to the team project: **Builder** and **Factory Method** in Java 21. This is the runnable domain foundation; the other four patterns and the web application are to be integrated by the team.

## Run and test

Install JDK 21 and set `JAVA_HOME`. The checked-in Maven wrapper downloads Maven automatically, with its distribution checksum verified.

```sh
./mvnw verify
java -jar target/hearth-and-table-1.0.0.jar
```

On Windows, use `mvnw.cmd verify`. `Main` demonstrates creating meals through their factories and building validated dine-in and takeaway orders. Prices are in KZT. GitHub Actions runs the tests and the console demonstration on pushes and pull requests.

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
└── builder/
    └── product/
```

`Main` is the only Java file directly at the package root. Tests follow the corresponding pattern packages. The factory has no dependency on the builder or on the remaining patterns. The builder depends on the `Meal` abstraction, allowing Decorator to wrap products later.

## Design choices

- **SRP:** menu definitions, product creation, order construction and the console demonstration have separate responsibilities.
- **OCP / DIP:** clients use `Meal` and `MealFactory`; adding a concrete creator does not change `Order`.
- **LSP / ISP:** the three meal types share one small product contract.
- **DRY:** `BaseMeal` holds common immutable product data; money and order validation have a single implementation.
- **KISS:** ordinary Java classes, records and collections; no framework is needed to demonstrate either pattern. The Maven parent supplies dependency versions for later Spring Boot integration.

## Team integration

| Member | GitHub | Assigned patterns |
| --- | --- | --- |
| Anuar | [ANAYSSA](https://github.com/ANAYSSA) | Builder, Factory Method — implemented here |
| Akmaral | [arynttai](https://github.com/arynttai) | Strategy, Bridge — to integrate |
| Mukhammad | [vertex-blip](https://github.com/vertex-blip) | Observer, Decorator — to integrate |

Teammates should add their implementations under `strategy`, `bridge`, `observer` and `decorator`, with role-specific subfolders. Keep `Main` as the sole root class and extend its demonstration after integration. Strategy can price the immutable order lines; Decorator can implement `Meal`; Observer can manage progress separately from the immutable order; Bridge can render receipts through an independent renderer interface.

Each member must use their own GitHub-linked commit email and commit real implementation changes. Merge their commits into `main` without replacing authors. GitHub's commit history shows authors directly; profile graphs and Contributors may update later because GitHub caches them.

This branch currently covers **two creational patterns**. It is not yet the completed six-pattern submission. The final team submission also needs structural/behavioral patterns, screenshots, UML, a PDF report, source ZIP and each student's individual submission by the course deadline.

The implementation adapts Anuar's existing Builder/Factory foundation from [Final-Project-SDP](https://github.com/ANAYSSA/Final-Project-SDP), with dependencies on the other four patterns removed. It continues the [Builder](https://github.com/ANAYSSA/SDPAssignment1Builder) and [Abstract Factory assignment](https://github.com/ANAYSSA/Assignment2SDP_AbstractFactory_AnuarM); this contribution explicitly demonstrates **Factory Method**.
