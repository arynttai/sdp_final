# Akmaral: Strategy and Bridge

Akmaral's part includes two patterns: **Strategy** is behavioral, and **Bridge** is structural. Strategy chooses a discount rule. Bridge creates receipts in different formats. Both parts use the same order as the rest of the project.

GitHub: [arynttai](https://github.com/arynttai). Original commits: [Strategy](https://github.com/arynttai/sdp_final/commit/f05a62e34cb57f9c0647b9963497f20861b0aa6e) and [Bridge](https://github.com/arynttai/sdp_final/commit/b78f7d6e3357910a7de4cd085047843221717ced).

## Strategy: choose a discount

`DiscountStrategy` is the interface. Its `code()` method gives the discount name. Its `discount(Order)` method returns the amount to subtract.

| Class | Code | Rule |
| --- | --- | --- |
| `NoDiscount` | `NONE` | 0% |
| `StudentDiscount` | `STUDENT` | 10% |
| `PickupDiscount` | `PICKUP` | 20%, takeaway only |

`PricingCalculator` uses the chosen strategy and creates a `Pricing` record. It rejects a discount below zero or above the order subtotal. A pickup discount on a dine-in order also gives an error.

This pattern keeps each rule in its own class. We can add another rule without changing the calculator.

Prices are in KZT. `BigDecimal` stores decimal amounts accurately. The code rounds money to two decimal places using `HALF_UP`: a third decimal digit of 5 or more rounds up.

- `subtotal` is the amount before a discount.
- `discount` is the amount removed.
- `Pricing.total()` is the amount to pay.

`Order.total()` still means the subtotal. For 4000 KZT with `STUDENT`, the discount is 400 KZT and the payable total is 3600 KZT.

[Strategy source](../src/main/java/com/restaurant/strategy) and [Strategy UML](../output/pdf/ITALIANO-Report.pdf).

## Bridge: choose a document and format

`ReceiptDocument` is the base class for two documents. `CustomerReceipt` shows items, prices, the discount and the final total. `KitchenTicket` shows items, quantities, service details and notes, without prices.

`ReceiptRenderer` is the format interface. `TextReceiptRenderer` makes plain text. `HtmlReceiptRenderer` makes HTML. `ReceiptContent` holds the data passed to a renderer. HTML output escapes user text so it cannot become HTML code.

A document receives its renderer in the constructor. Two document types and two formats give four combinations. This avoids a separate class for every combination. We can add a format without changing both document classes.

[Bridge source](../src/main/java/com/restaurant/bridge) and [Bridge UML](../output/pdf/ITALIANO-Report.pdf).

## Small example

Here, `order` is an existing `Order` made by `Order.Builder`. Use pricing from that same order.

```java
Pricing pricing = new PricingCalculator()
    .calculate(order, new StudentDiscount());
String receipt = new CustomerReceipt(new TextReceiptRenderer())
    .render(order, pricing);
System.out.println(receipt);
```

## Team integration

Anuar's Factory Method creates a meal. Mukhammad's Decorator adds toppings. Anuar's Builder creates the order. Strategy calculates the price, and Bridge prints its documents. Observer sends status information to the customer and kitchen parts.

The project has six pattern packages and `Main` outside them. Together, the team covers creational, structural and behavioral patterns.

## Design ideas for the defense

- **OOP:** interfaces allow different implementations. Inheritance connects receipt classes. Composition means a document contains a renderer.
- **SRP:** discount classes calculate discounts; renderers format documents.
- **OCP:** new rules or formats can use the existing interfaces.
- **LSP:** either renderer can work with either document.
- **ISP:** the interfaces have only a few necessary methods.
- **DIP:** the calculator and documents depend on interfaces.
- **KISS:** use simple classes and two clear choices for receipts.
- **DRY:** `ReceiptDocument` shares common details and item formatting.

## Show the result

Use JDK 21 or newer. From the project folder in Windows PowerShell:

```powershell
.\mvnw.cmd package
java -jar target/italiano-1.0.0.jar --demo
```

[Main](../src/main/java/com/restaurant/Main.java) demonstrates all six patterns. For a 3400 KZT takeaway order, the three strategies give totals of 3400, 3060 and 2720 KZT. It shows all four receipt combinations. The combined example checks 4700 KZT before discount, 4230 KZT after discount and eight notifications.

The PDF report must include code examples, UML and screenshots. Submit it with the source ZIP and repository link. At the defense, explain the choices and show the output.

Pattern explanations: [Strategy](https://refactoring.guru/design-patterns/strategy), [Bridge](https://refactoring.guru/design-patterns/bridge).
