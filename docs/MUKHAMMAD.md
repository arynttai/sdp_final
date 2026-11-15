# Mukhammad: Decorator, Observer, and the website

Mukhammad's part has Decorator, a structural pattern, and Observer, a behavioral
pattern. They add toppings and send order updates. This part also connects the
website to Java.

## Decorator: adding toppings

Decorator combines toppings without a new class for every meal choice.
A decorator holds another object and adds behavior to it.

The main classes are:

- `Meal`: the common interface for meals and decorated meals.
- `ToppingDecorator`: the base decorator. It holds a `Meal` and adds a topping's
  name and price. The meal's ID and category stay the same.
- `ExtraCheese`, `Mushrooms`, and `Olives`: the three concrete decorators.
- `ToppingType`: stores topping names, IDs, and prices in one place.
- `ToppingCatalog`: creates the selected decorators and checks the choices.

Decorators can wrap other decorators. Toppings work with pizza and pasta, but
not drinks. The catalog accepts up to three different, known topping IDs.

```java
Meal pizza = new MenuCatalog().create("margherita");
Meal withCheese = new ExtraCheese(pizza);
System.out.println(withCheese.price());
```

A Margherita costs 2000 KZT. Cheese adds 350 KZT, so this example prints 2350.00.
Two pizzas cost 4700 KZT. Akmaral's 10% student discount removes 470 KZT, leaving
4230 KZT to pay. The Builder accepts the decorated object because it still
implements `Meal`.

See the [Decorator source](../src/main/java/com/restaurant/decorator) and
[UML diagram in the report](../output/pdf/ITALIANO-Report.pdf).

## Observer: updating order status

Observer sends different messages to the customer and kitchen through one
interface.

`OrderProgress` is the subject. It stores the current status and its observers.
`OrderObserver` defines the `onStatusChanged` method. `CustomerObserver` and
`KitchenObserver` implement this method. `StatusChange` holds the order ID,
status, and time. Each observer returns an `OrderNotification` with its message.

The application subscribes both observers before calling `announce()`. This
sends the first messages for `PLACED`. The full sequence is:

```text
PLACED -> PREPARING -> READY -> COMPLETED
```

Each status sends two messages, so four statuses produce eight messages.
Statuses move one step at a time. Repeating the current status adds no messages.
Observers can also be removed with `unsubscribe()`.

These are Java method calls. Messages stay inside the application; the project
does not send real SMS or email. The website reads the stored messages.

See the [Observer source](../src/main/java/com/restaurant/observer) and
[UML diagram in the report](../output/pdf/ITALIANO-Report.pdf).

## How the parts work together

The order flow is Factory Method -> Decorator -> Builder -> Strategy. Observer
then handles status messages, and Bridge creates receipts. Each team member
covers two patterns. Together, the six patterns cover creational, structural,
and behavioral design.

The package root has `Main.java` and six folders: `factory`, `builder`,
`decorator`, `strategy`, `observer`, and `bridge`. The web service classes are
inside `builder`.

The website uses HTML, CSS, and JavaScript. It sends order requests to the Java
API. Java checks the choices and calculates prices. The website shows the
results, receipts, and order history. Orders are kept in memory for each browser
session. This is a demonstration without real payments.

Examples: [desktop menu](../output/pdf/ITALIANO-Report.pdf),
[mobile menu](../output/pdf/ITALIANO-Report.pdf), and
[order history](../output/pdf/ITALIANO-Report.pdf).

## Design principles

- **OOP:** private fields protect state. Interfaces hide details.
  Concrete toppings inherit from `ToppingDecorator`. Different meals work
  through the same `Meal` methods; this is polymorphism.
- **SRP:** decorators add toppings; observers create messages; `OrderProgress`
  controls status changes. Each has a clear job.
- **OCP and DIP:** the Builder uses `Meal`, and the subject uses `OrderObserver`.
  A new implementation can use these interfaces without changing the code that
  uses them.
- **DRY:** `ToppingType` holds topping prices once. The website and decorators
  use this shared data.
- **KISS:** ordinary Java objects and lists are enough for this demonstration.

## Run and explain the project

Use JDK 21 or newer and set `JAVA_HOME` to it. On Windows PowerShell, run:

```powershell
.\mvnw.cmd package
java -jar target/hearth-and-table-1.0.0.jar --demo
java -jar target/hearth-and-table-1.0.0.jar
```

On Linux or macOS, use `./mvnw package` for the first command. Open
[localhost:8080](http://localhost:8080) after starting the website.

`Main --demo` runs all six patterns without starting the server. It checks the
4700 KZT subtotal, 4230 KZT total, and eight notifications. It prints a receipt
and stops with an exception if these checks fail.

For the defense and report, explain the patterns, their class roles, and UML.
Show how the interfaces connect the team's work.

## Contribution links

- [Decorator](https://github.com/arynttai/sdp_final/commit/8c4123127b3f314aec87a34f7a6a2f8ddf4da235)
- [Observer](https://github.com/arynttai/sdp_final/commit/f70398df5b4366f771b272726f84981282788d58)
- [Website integration](https://github.com/arynttai/sdp_final/commit/2a125370d2c5e5c178f310963184a364225e32ee)

Pattern explanations: [Decorator](https://refactoring.guru/design-patterns/decorator), [Observer](https://refactoring.guru/design-patterns/observer).
