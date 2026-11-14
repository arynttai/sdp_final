# ITALIANO — Restaurant Ordering System

Team project in Java 21 and Spring Boot with six integrated design patterns:
Anuar's **Builder** and **Factory Method**, Akmaral's **Strategy** and **Bridge**,
and Mukhammad's **Observer** and **Decorator**. The same application serves the
restaurant website and its Java API. Orders are a session-scoped, in-memory
coursework demonstration; there are no real payments or SMS/email deliveries.

## Run and test

Install JDK 21 and set `JAVA_HOME`. The checked-in Maven wrapper downloads Maven automatically, with its distribution checksum verified.

```sh
./mvnw verify
java -jar target/hearth-and-table-1.0.0.jar --demo
java -jar target/hearth-and-table-1.0.0.jar
```

Open [http://localhost:8080](http://localhost:8080) after the normal JAR launch.
On Windows PowerShell, use `.\mvnw.cmd verify`; the Java commands are the same.
`--demo` exercises all six patterns and exits without starting the web server.
Prices are in KZT. The GitHub Actions workflow is configured to run the tests
and `--demo` on pushes and pull requests.

## Website

ITALIANO has an English interface with dark espresso and light ivory themes.
The header switch saves the selected theme locally. The two supplied ITALIANO
wordmarks appear in their matching themes, and HTML receipts follow the active
theme. Product dialogs show the photograph, base price, optional toppings,
quantity, and quoted total together. Decorative dividers, repeated slogans,
and motion controls are omitted.

The menu uses real Unsplash food photographs, clear prices, straightforward
category navigation, and a persistent basket. The six menu photographs are
served at 1600 pixels wide for the larger product dialogs. Photography, logo artwork,
fonts, and interaction code are served locally.

The customer can filter the menu, choose quantities and toppings, edit the
basket, select dine-in or takeaway and a discount, obtain a server quote,
and place an order. Each order shows its current status, items, total, and receipt.
The order details disclosure contains customer history from Java Observer notifications;
its demo controls disclosure provides kitchen history, the remaining document
formats, and an explicitly labelled status advance. Customer and kitchen documents
use Akmaral's Bridge in HTML or text. API data, document content, and pricing
remain authoritative. Theme changes preserve the basket and form inputs.

See [Mukhammad's implementation and verification notes](docs/MUKHAMMAD.md).
Image sources are recorded in
[`images/CREDITS.txt`](src/main/resources/static/images/CREDITS.txt). Font
licenses are included alongside the local font files:
[`Bricolage Grotesque`](src/main/resources/static/fonts/OFL-BricolageGrotesque.txt)
and [`DM Sans`](src/main/resources/static/fonts/OFL-DMSans.txt).

## Final submission

The [final report](output/pdf/ITALIANO-Final-Report.pdf) explains all six patterns,
their integration, UML, code examples, contributions, verification, and defense
topics. Its editable generator and diagram sources are in `docs/report/`.
The Java package root contains only the six pattern packages and `Main.java`.
The original contributions by Anuar, Akmaral, and Mukhammad remain in Git history.

Submit the PDF report, source ZIP, and repository link through Moodle. Each
student must submit individually and attend the defense. The supplied course
page lists the deadline as 15 November 2026, 23:59; its weekday label is
inconsistent, so check the date/time in Moodle when submitting.

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
│   ├── product/
│   ├── application/      # orchestration and DTOs
│   ├── repository/       # one order book per HTTP session
│   └── web/              # HTTP adapters
├── strategy/
│   ├── api/
│   ├── concrete/
│   └── context/
├── bridge/
│   ├── abstraction/
│   └── implementation/
├── decorator/
│   ├── component/
│   ├── concrete/
│   └── catalog/
└── observer/
    ├── api/
    ├── subject/
    ├── concrete/
    └── event/
```

`Main` is the only Java file directly at the package root. Tests follow the
corresponding pattern packages. The factory has no dependency on the builder
or on the remaining patterns. The builder depends on the `Meal` abstraction,
which the topping decorators implement. Web/application adapters live under
`builder` to preserve the six-package coursework structure; domain products,
strategies, renderers, and observers remain independent of HTTP.

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

## Mukhammad's two patterns

| Pattern | Role | Implementation |
| --- | --- | --- |
| Decorator | Shared wrapper | `ToppingDecorator` implements and wraps `Meal` |
| Decorator | Concrete toppings | `ExtraCheese` +350, `Mushrooms` +300, `Olives` +300 KZT |
| Decorator | Metadata / registration | `ToppingType`, `ToppingCatalog` |
| Observer | Contract | `OrderObserver.onStatusChanged` |
| Observer | Subject | Per-order `OrderProgress`, with subscribe/unsubscribe and validated transitions |
| Observer | Subscribers | `CustomerObserver`, `KitchenObserver` |
| Observer | Event values | `StatusChange`, `OrderNotification`, `OrderStatus` |

Toppings preserve the wrapped meal's ID and category and cannot decorate drinks.
The catalog accepts at most three distinct known topping IDs. Two Margheritas
with Extra cheese have subtotal 4700 KZT and STUDENT total 4230 KZT.
The order subject announces placement, then advances through
`PLACED → PREPARING → READY → COMPLETED`. Each current subscriber produces one
notification per publication. Repeating the same requested status is idempotent;
invalid transitions do not publish events.

## API and order privacy

| Method | Path | Purpose |
| --- | --- | --- |
| GET | `/api/health` | Health status |
| GET | `/api/menu` | Meals, toppings, discounts, currency, and status sequence |
| POST | `/api/orders/quote` | Server pricing without storing an order |
| POST | `/api/orders` | Place an order with a UUID `requestId` |
| GET | `/api/orders` | Current session's orders |
| GET | `/api/orders/{id}` | One order in the current session |
| POST | `/api/orders/{id}/advance` | Advance the demo by one valid status |
| POST | `/api/orders/{id}/status` | Request a status; a repeated current status adds no events |
| GET | `/api/orders/{id}/receipt?type=customer&format=html` | Bridge receipt; `type=customer\|kitchen`, `format=html\|text` |

Send JSON with `Content-Type: application/json` for POST requests. The server
validates inputs and returns a JSON `message` on errors. Each HTTP session has
its own order book, limited to 20 orders. Another session cannot read, advance,
or render receipts for these orders. Orders disappear when the session expires
(configured timeout: 60 minutes) or the server restarts. API responses use
`Cache-Control: no-store`.

Retrying the same `requestId` and payload returns the existing order, including
under concurrent requests. Reusing that ID with different order details returns
a conflict. Pricing and the immutable order are stored together, so receipts
use the same discounted total returned at placement.

## Design choices

- **SRP:** menu definitions, product creation, order construction, status publication, HTTP handling, and rendering have separate responsibilities.
- **OCP / DIP:** clients use `Meal` and `MealFactory`; adding a concrete creator does not change `Order`.
- **LSP / ISP:** the three meal types share one small product contract.
- **DRY:** `BaseMeal` holds common immutable product data; money and order validation have a single implementation.
- **KISS:** the patterns use ordinary Java classes, records, and collections. Spring Boot supplies the web boundary and serves one HTML/CSS/JavaScript frontend.

## Team integration

| Member | GitHub | Assigned patterns |
| --- | --- | --- |
| Anuar | [ANAYSSA](https://github.com/ANAYSSA) | Builder, Factory Method — implemented here |
| Akmaral | [arynttai](https://github.com/arynttai) | Strategy, Bridge — implemented here |
| Mukhammad | [vertex-blip](https://github.com/vertex-blip) | Observer, Decorator, website integration — implemented here |

The integrated flow is **Factory → Decorator → Builder → Strategy**, followed
by Observer status notifications and Bridge receipts. The original Factory,
Builder, Strategy, and Bridge contracts and their existing tests are preserved.
See [Akmaral's integration notes](docs/AKMARAL.md#integration-for-mukhammad) and
[Mukhammad's notes](docs/MUKHAMMAD.md) for the extension points.

Each member must use their own GitHub-linked commit email and commit real implementation changes. Merge their commits into `main` without replacing authors. GitHub's commit history shows authors directly; profile graphs and Contributors may update later because GitHub caches them.

The current implementation covers **two creational, two behavioral, and two
structural patterns**. `./mvnw verify` passed 63 tests in the current integration
run, including domain, HTTP, idempotency, session-isolation, and package-layout
checks. Desktop and mobile browser checks passed for the complete ordering flow,
keyboard dialogs, receipts, and error recovery; see [Mukhammad's notes](docs/MUKHAMMAD.md).
The reviewed English screenshots, seven UML diagrams, report generator, and
combined PDF report are included. Export a source archive from the final commit
with `git archive --format=zip --prefix=ITALIANO/ --output=ITALIANO-source.zip HEAD`.
Each student still needs to submit the files and repository link to Moodle and
complete the oral defense.

Anuar's initial foundation adapted Builder/Factory from [Final-Project-SDP](https://github.com/ANAYSSA/Final-Project-SDP), removing dependencies on the other four patterns before the team's separate integrations. It continues the [Builder](https://github.com/ANAYSSA/SDPAssignment1Builder) and [Abstract Factory assignment](https://github.com/ANAYSSA/Assignment2SDP_AbstractFactory_AnuarM); this contribution explicitly demonstrates **Factory Method**.
