# ITALIANO: Restaurant Ordering System

[Website](https://hearth-table.178.62.244.88.sslip.io/) | [Repository](https://github.com/arynttai/sdp_final)

A restaurant ordering app built with Java 21, Spring Boot, HTML, CSS and JavaScript.
Users choose meals and toppings, place orders, view status updates and open receipts.
Prices are in KZT. Orders stay in memory for each browser session. There are no real payments.

## Run

Use JDK 21 or newer. Set `JAVA_HOME` and make sure `java -version` shows the right version.

Build from the project folder in Windows PowerShell:

```powershell
.\mvnw.cmd package
```

On Linux or macOS, use `./mvnw package`.

Show all six patterns through `Main`:

```sh
java -jar target/italiano-1.0.0.jar --demo
```

The demo shows six steps: create a meal, add cheese, build an order, choose a
discount, notify observers and print a receipt. It checks order totals and eight
customer and kitchen notifications.
Two Margheritas with cheese cost 4700 KZT, or 4230 KZT with the student discount.

Start the website:

```sh
java -jar target/italiano-1.0.0.jar
```

Open [localhost:8080](http://localhost:8080). In IntelliJ IDEA, run `Main` with
`--demo` for the console example, or without it for the website.

## Patterns and team

| Pattern | Type | Purpose | Member |
| --- | --- | --- | --- |
| Factory Method | Creational | Creates pizza, pasta and drink objects | Anuar |
| Builder | Creational | Builds an order step by step | Anuar |
| Strategy | Behavioral | Chooses a discount rule | Akmaral |
| Bridge | Structural | Creates customer and kitchen receipts in text or HTML | Akmaral |
| Decorator | Structural | Adds toppings to meals | Mukhammad |
| Observer | Behavioral | Sends order updates to the customer and kitchen | Mukhammad |

Factory Method creates a meal. Decorator adds toppings. Builder creates the order.
Strategy calculates the price. Observer handles status updates, and Bridge creates receipts.

The six packages are in `src/main/java/com/restaurant/`, with `Main.java` at their root.
The website sends requests to the Java API, which handles prices and orders.

## Documentation

- [Anuar](docs/ANUAR.md): Factory Method and Builder. GitHub: [ANAYSSA](https://github.com/ANAYSSA).
- [Akmaral](docs/AKMARAL.md): Strategy and Bridge. GitHub: [arynttai](https://github.com/arynttai).
- [Mukhammad](docs/MUKHAMMAD.md): Decorator, Observer and the website. GitHub: [vertex-blip](https://github.com/vertex-blip).
- [PDF report](output/pdf/ITALIANO-Report.pdf): UML diagrams, code examples and screenshots.

The member notes explain the classes, OOP, SOLID, KISS and DRY with examples.
Font licenses: [Bricolage Grotesque](src/main/resources/static/fonts/OFL-BricolageGrotesque.txt)
and [DM Sans](src/main/resources/static/fonts/OFL-DMSans.txt).
