# Nilambar ERP — Ordering Application

ERP-style e-commerce ordering application: mobile-OTP registration, address book with coordinates,
a 50-product catalog, cart and checkout, and a 5 km home-delivery rule enforced server-side.

**Stack:** Java 17 · Spring Boot 3.2 · Spring MVC + JSP/JSTL · Spring Security · Spring Data JPA ·
Flyway · MySQL 8 · Apache Kafka · Maven (`war` packaging).

## Prerequisites

- JDK 17
- Maven 3.6+
- Docker + Docker Compose (MySQL, Kafka, and the Testcontainers-based integration tests)

## Run it

```bash
cd erp-app
docker compose up -d          # MySQL on :3307, Kafka on :9092
mvn spring-boot:run           # http://localhost:8080
```

Flyway applies the schema and seed data (2 stores, 50 products) on the first start.

> **Packaging note:** JSPs cannot be served from an executable fat jar, so the module is packaged as
> a `war` with `spring-boot-starter-tomcat` and `tomcat-embed-jasper` in `provided` scope.
> `mvn spring-boot:run` puts them back on the classpath, so JSP rendering works in development.
> For a container deployment, build with `mvn package` and drop `target/erp-app.war` into a
> standalone Tomcat 10.1 (Jakarta EE 10), or run `java -jar` only after switching to a template
> engine that supports fat jars.

### Signing in (development)

There is no SMS provider. `LoggingOtpSender` prints the code to the application log:

```
=== DEV OTP === mobile=9876543210 code=418293 ===
```

Enter any valid 10-digit Indian mobile number, grab the code from the log, and verify. The first
successful verification creates the account and forces the profile + address step.

Seeded stores are in Bengaluru, so use coordinates near **12.9756, 77.6050** to see home delivery
accepted, and something like **13.20, 77.80** to see it refused with store pickup offered.

## Tests

```bash
mvn verify
```

- Unit tests: OTP lifecycle (hashing, expiry, attempt counter, rate limit), Haversine and the
  delivery-radius decision, cart totals and stock guards, order placement (stock decrement,
  radius enforcement, payment decline).
- Integration test (`RegistrationToOrderIT`): the whole registration → address → cart → checkout →
  order path against **real MySQL and Kafka containers** via Testcontainers, including the
  assertion that the Kafka consumer moves the order out of `PLACED`.

## Configuration

Everything is overridable by environment variable; no secrets live in the repo.

| Property | Env var | Default |
| --- | --- | --- |
| `spring.datasource.url` | `DB_URL` | `jdbc:mysql://localhost:3307/erpdb` |
| `spring.datasource.username` / `.password` | `DB_USERNAME` / `DB_PASSWORD` | `erp` / `erppass` |
| `spring.kafka.bootstrap-servers` | `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` |
| `erp.delivery.radius-km` | — | `5` |
| `erp.delivery.fee-per-slab` / `erp.delivery.fee-slab-km` | — | `30.00` / `1.5` |
| `erp.delivery.pickup-fee` | — | `0.00` |
| `erp.payment.account-number` / `.ifsc` / `.payee-name` / `.bank-name` | — | `11540334561` / `SBIN0007021` / `Nilambar ERP` / `State Bank of India` |
| `erp.payment.upi-id` | — | empty (falls back to a bank-transfer QR) |
| `erp.otp.ttl-minutes` / `.max-attempts` | — | `5` / `5` |
| `erp.otp.max-requests-per-window` / `.rate-limit-window-minutes` | — | `3` / `15` |
| `erp.otp.sender` | — | `logging` (`sms` selects the unimplemented gateway stub) |
| `erp.catalog.page-size` | — | `12` |

## Domain model

```
users ──< addresses                stores
  │                                  │
  ├──── carts ──< cart_items >── products ──< order_items >── orders ──┘
  │
  └──< orders           otp_codes, otp_request_logs, processed_events
```

`processed_events` gives the Kafka consumer idempotency; `otp_request_logs` backs the rate limit.

## Kafka

| Topic | Produced when | Payload |
| --- | --- | --- |
| `erp.otp.requested` | an OTP is generated | `{eventId, mobile, channel, requestedAt}` — never the code |
| `erp.user.registered` | a new user row is created | `{eventId, userId, mobile, registeredAt}` |
| `erp.order.placed` | an order is committed | `{eventId, orderId, orderNumber, userId, fulfilmentType, distanceKm, total, lines[], placedAt}` |

`OrderEventsConsumer` listens on `erp.order.placed`, skips event ids already in `processed_events`,
and advances the order status. `OrderProgressScheduler` then walks open orders
`PLACED → CONFIRMED → OUT_FOR_DELIVERY → DELIVERED` (`READY_FOR_PICKUP → PICKED_UP` for pickup
orders) every 30 s so the lifecycle is visible without a logistics integration. Publishing failures are logged,
never propagated, so a broker outage cannot fail a checkout.

## Delivery radius

`DeliveryService` computes the great-circle (Haversine) distance from the selected address to every
active store and takes the nearest. Within `erp.delivery.radius-km` home delivery is offered;
otherwise the UI shows the actual distance and only store pickup is selectable. `OrderService`
recomputes the quote at placement time, so posting `fulfilmentType=HOME_DELIVERY` from a modified
form is rejected.

The delivery charge is slab-based: `erp.delivery.fee-per-slab` (₹30) is charged for every started
`erp.delivery.fee-slab-km` (1.5 km) slab, so 0–1.5 km costs ₹30, 1.5–3 km ₹60, and 4.5–5 km ₹120.
The fee shown on checkout comes from the same `DeliveryService.deliveryFee` call that `OrderService`
uses when it recomputes the quote, so the browser cannot influence it. Store pickup uses
`erp.delivery.pickup-fee`.

## Payment details and QR

Checkout and every order page show the collection account (`erp.payment.*`) plus a QR code rendered
on the fly by `PaymentQrService` (ZXing) at `/checkout/payment-qr.png` and
`/orders/{id}/payment-qr.png`. The QR encodes a UPI intent when `erp.payment.upi-id` is set,
otherwise the beneficiary/account/IFSC/amount/reference as plain text. Amounts are always computed
server-side from the cart or the persisted order, never from a request parameter.

## Customer feedback

Once an order reaches `DELIVERED` or `PICKED_UP`, its detail page offers a 1–5 rating with an
optional comment, stored one-per-order in `order_feedback` (`FeedbackService` enforces both the
status precondition and the single-review rule).

## Product images

The 50 catalog images are **generated locally**, not downloaded: `tools/generate_catalog.py` draws
600×600 gradient tiles stamped with the SKU (pure-Python PNG encoding, no dependencies) and writes
the matching `V3__seed_products.sql`. Re-running it is deterministic. Swap in real photography by
replacing the files in `src/main/resources/static/images/products/`.

```bash
python3 tools/generate_catalog.py
```

## What is stubbed

- **SMS delivery** — `LoggingOtpSender` logs the OTP; `SmsGatewayOtpSender` is a placeholder that
  throws until a provider is wired in.
- **Payments** — `MockPaymentService` always succeeds and returns a `MOCKPAY-…` reference;
  `setForceFailure(true)` exercises the decline path in tests. The bank details and QR are shown for
  a real out-of-band transfer but nothing reconciles them.
- **Fulfilment** — the status machine is driven by a scheduler, not a courier integration.
