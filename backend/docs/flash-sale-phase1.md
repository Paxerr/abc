# Platform-funded Flash Sale — Phase 1

Flash Sale discounts the customer's payable amount while preserving the Vendor's receivable from the verified original price. This implementation extends the existing Booking and Order engines.

Scope: local branch `feat/flash-sale`, baseline `3c80558`. No commit, push, PR, production deployment or migration of `danasea` / `danasea_demo` is authorized by Phase 1.

## Architecture and migration

- Domain: `modules/flashsale/domain` defines pricing, immutable snapshots and the effective time window.
- Application: `FlashSaleService` coordinates administration, policy lookup, public offers and promotional reservations through `FlashSaleStore`.
- Persistence: `JdbcFlashSaleStore` uses the same DataSource and REQUIRED transaction as Booking's JPA adapter. No independent financial transaction is opened for reservation.
- Booking: `FlashSaleBookingPort` adds explicit offer selection, durable idempotency and price reservation to `CreateBookingHoldUseCase`.
- Both `CreateOrderUseCase` and `OrderPaymentService.createOrder` use the stored original-price commission snapshot.
- Catalog: additive `flashSales` arrays on existing list/detail responses identify the discounted Option. Other Options keep their own price.

Baseline migrations were listed before assigning V39. `V39__platform_funded_flash_sales.sql` creates `vendor_commission_policies`, `flash_sales`, `flash_sale_allocations` and `flash_sale_hold_requests`. V1–V38 are unchanged. V39 seeds no users, Vendors, Services or financial policies. It requires PostgreSQL `btree_gist` for an exclusion constraint preventing overlapping ENABLED offers on one Option.

Money uses NUMERIC/BigDecimal with two decimal places, matching existing financial tables. Commission rounds once on the original BookingItem amount, HALF_UP. The unit subsidy ceiling rounds down conservatively, including its commission basis, so a quantity increase cannot spend an extra rounding cent. Percentage display uses four decimal places. All Flash Sale instants are stored as TIMESTAMPTZ and returned in UTC; a client can display them in `Asia/Ho_Chi_Minh`.

## Commission configuration

Policies are append-only, versioned per Vendor and carry a required provenance label. There are no seeded/default rates, subsidy ratios, operating costs or retained margins. A missing policy returns `COMMISSION_POLICY_MISSING`; DRAFT creation is allowed, activation is blocked.

ADMIN creates a policy using `POST /api/admin/flash-sales/commission-policies`:

```json
{
  "vendorId": "<vendor UUID>",
  "rate": 0.10,
  "maximumRatio": 0.80,
  "unitCost": 5000,
  "minimumMargin": 10000,
  "provenance": "DEMO_LOCAL_TEST_ONLY"
}
```

These numbers are explicitly test data, not an approved business policy. Server generates the policy ID and next version. Rate and maximumRatio must be in [0,1], at most six decimal places. Cost/margin must be nonnegative valid money. Provenance is required, maximum 200 characters.

Activation freezes the policy ID and original Option price. A later policy version cannot reprice existing reservations. If an Option price changes, the old offer is hidden from active/public promotion queries and new promotional holds fail explicitly. Existing snapshots remain valid.

**Compatibility consideration:** normal Order creation and voucher preview also resolve the configured commission source. Configure each participating Vendor before applying this code to an existing runtime; missing configuration now fails instead of silently using 10%. Ordinary Service discovery and Booking Holds do not require a commission policy.

Settlement also requires a stored commission rate or an explicitly supplied policy rate. Legacy rows missing their commission snapshot are blocked for review; the system does not reconstruct history using an invented 10% rate.

## API inventory

JSON requests use `Content-Type: application/json`. ADMIN routes require a verified Bearer JWT with ADMIN role. Guest public GETs require no Authorization header. Existing authentication and rate-limit filters remain active.

| Method | Route | Result |
|---|---|---|
| GET | `/api/admin/flash-sales/eligible-services?page=0&size=20` | Published/active eligible Options |
| POST | `/api/admin/flash-sales/preview` | Option, financial calculation, configured cost/margin and provenance |
| POST | `/api/admin/flash-sales` | Create DRAFT, HTTP 201 |
| GET | `/api/admin/flash-sales?page=0&size=20` | Admin details and allocation usage |
| GET | `/api/admin/flash-sales/{id}` | Detail and policy issues |
| PATCH | `/api/admin/flash-sales/{id}` | Replace editable DRAFT terms |
| DELETE | `/api/admin/flash-sales/{id}` | Delete DRAFT only, HTTP 204 |
| POST | `/api/admin/flash-sales/{id}/activate` | Enable now or for future scheduling |
| POST | `/api/admin/flash-sales/{id}/cancel` | Prevent new allocations, preserve valid holds |
| POST | `/api/admin/flash-sales/commission-policies` | Append Vendor policy version |
| GET | `/api/admin/flash-sales/commission-policies?vendorId={id}` | Vendor policy history |
| GET | `/api/flash-sales/active` | Guest-visible, effective and purchasable offers |
| GET | `/api/flash-sales/{id}` | Effective offer; SOLD_OUT detail when exhausted |

Pagination requires page 0..100000 and size 1..100. Admin usage separates reserved, consumed and released quantities/budget. Released totals are historical and may exceed the campaign quota after repeated reservation/release cycles.

Preview request: `serviceId`, `optionId`, `unitSubsidy`. Creation/PATCH body:

```json
{
  "serviceId": "<service UUID>",
  "optionId": "<option UUID>",
  "unitSubsidy": 30000,
  "startsAt": "2026-11-01T09:17:00+07:00",
  "endsAt": "2026-11-01T16:30:00+07:00",
  "quota": 10,
  "budget": 300000
}
```

All fields are required. `startsAt < endsAt`; `endsAt > serverNow` at creation/edit/activation. Quota/budget/subsidy are positive. Starts may be in the past for immediate activation. There is no fixed duration. PATCH is a complete DRAFT terms replacement; ENABLED/CANCELLED terms are immutable. Cancellation and repeated activation are idempotent as documented by their current state.

Public responses include only offer/Service/Option IDs, Service name/image, verified original price, sale price, true discount amount/percentage, startsAt/endsAt, purchasable remaining quantity, availability and VND currency. They contain no commission, budget, policy provenance, cost or margin. DRAFT/future/expired/cancelled offers are hidden; active list also excludes sold-out and stale-price offers. Detail returns 404 for hidden offers and `availability: SOLD_OUT` for an effective exhausted offer.

Typical statuses: 200 read/action, 201 create/Hold, 204 delete/cancel Hold, 400 malformed JSON/types, 401 missing JWT, 403 wrong role/ownership, 404 hidden/missing resource, 409 enabled overlap or idempotency-key conflict, 422 invalid business terms/policy/availability. Error body follows `{ "code": "COMMISSION_POLICY_MISSING", "message": "COMMISSION_POLICY_MISSING" }`.

## Booking and Order contract

Use existing `POST /api/bookings/hold` with CUSTOMER authentication and an `Idempotency-Key` header for a promotional hold:

```json
{
  "items": [{
    "slotId": "<slot UUID>",
    "optionId": "<option UUID>",
    "quantity": 2,
    "flashSaleId": "<flash-sale UUID>"
  }]
}
```

The header is required, nonblank, at most 120 characters for promotional Holds. Retrying the same customer/key/payload returns the original Booking and state. Reusing a key for another payload fails 409. Ordinary Holds keep their existing request contract. Omitting flashSaleId requests the ordinary price; selecting an unavailable offer fails explicitly and never silently switches to a higher price. Client-supplied final prices are not used.

Quantity is BookingItem quantity: persons for PER_PERSON and packages for PER_PACKAGE. The existing maximum 20 request items and total quantity 100 remain. Allocation stores original unit price/amount, quantity/pricing unit, policy ID/version/rate/commission, unit/total subsidy, customer payable, Vendor receivable and platform gross contribution. Funding is PLATFORM, enforced by its immutable campaign.

BookingItem price is the customer unit price. Order unit/subtotal/commission basis use the original price; the subsidy is stored separately as platform discount. Vendor discount is zero. No extra commission is charged on the reduced price. HOLD/PENDING_PAYMENT contain financial estimates/reservations, not evidence of revenue or Vendor payout.

For the explicitly labelled test policy above: original 500000, commission 50000, subsidy 30000 → customer 470000, Vendor receivable 450000, platform gross contribution 20000. Contribution is before operating costs, not net profit.

Voucher stacking is rejected in both Order creation and discount preview for a Booking containing Flash Sale allocations. No second Voucher or Booking engine is introduced.

## Concurrency, expiration and recovery

PostgreSQL serializes campaign reservations, checks quota/budget again in an insert trigger, enforces enabled time-range exclusion, and freezes allocation snapshot fields. Campaign locks are acquired in stable UUID order. Customer/key advisory locks serialize concurrent retries. Deferred BookingItem foreign keys allow reservation and Booking persistence to commit atomically.

Inventory still uses existing atomic Redis Lua scripts. Reservation occurs before Redis acquisition; persistence/commit failure rolls back PostgreSQL and releases Redis holds through compensation/transaction callbacks. An ambiguous Redis response is compensated as well. If Redis remains unavailable, its existing hold expiration provides a bounded fallback; this is not a distributed two-phase commit.

Booking status transitions update allocations in the same database transaction: RESERVED → CONSUMED on CONFIRMED; RESERVED → RELEASED on CANCELLED/REJECTED. Terminal allocations cannot transition again. Flash Sale expiry/cancellation does not invalidate a legitimate unexpired Hold. Confirmation still requires the existing successful payment checks.

`ExpireBookingHoldUseCase` locks/rechecks a candidate before expiry, releases Redis after commit and publishes the existing expiry event inside the transaction. A stale scheduler candidate cannot cancel a Booking that was already confirmed. After confirmed payment, refund processing retains consumed promotion history; it does not reopen quota. Existing refund/settlement calculations use customer cash and the original commission basis, reversing funding proportionately.

## Verification environment and evidence

Only `danasea_flashsale_test` on the existing PostgreSQL container and Redis logical database 15 are writable test resources. Integration tests assert the exact DB name before any fixture reset. They never create containers/volumes or mutate `danasea` / `danasea_demo`.

`FlashSaleHttpIntegrationTest` starts the newly compiled Phase 1 application with RANDOM_PORT and uses Java HttpClient over real TCP. `localhost:8081` is not used as implementation evidence. Authentication, HTTP controllers, PostgreSQL, Flyway, transactions and Redis Lua execution are real. Rabbit/email adapters are mocked to prevent outbound delivery; payment callbacks use the existing test-profile endpoint with valid signatures and controlled fixtures, not an external money transfer.

The 24-Service regression reads public catalog fields from `danasea_demo` in a verified READ ONLY, REPEATABLE READ transaction. It copies those fields into the guarded test DB; no passwords, bank details or safety attestations are copied. It compares all original UUIDs, then exercises details, all 33 Options, future slots, ordinary Holds and cancellations on the Phase 1 runtime.

Set `FLASH_SALE_TEST_JDBC_URL=jdbc:postgresql://localhost:5434/danasea_flashsale_test` and provide `FLASH_SALE_TEST_DB_PASSWORD` through the process environment. Integration tests intentionally skip without the explicit guarded URL; a skipped test is not acceptance evidence. Run from `backend`:

```text
mvn -Dtest=FlashSalePricingTest,FlashSaleHttpIntegrationTest,BookingDomainTest,DiscountAllocationEngineTest,BookingHoldCleanupJobTest,CreateBookingHoldUseCaseTest,CancelBookingHoldUseCaseTest,ConfirmBookingUseCaseTest,CreateOrderUseCaseTest,DiscountManagementIntegrationTest,OrderPaymentServiceTest,SettlementCalculationTest,BookingHoldControllerTest test
```

Artifacts: `target/flash-sale-http-evidence.jsonl`, `target/flash-sale-tests.log`, `target/surefire-reports/`. The first HTTP evidence row identifies baseline, runtime port, target DB and Redis database; subsequent rows record method/path/role/status/response without tokens.

Before any Phase 2 application: obtain explicit approval for the target DB/runtime, review V39 and btree_gist availability, configure labelled Vendor policies, preserve existing Flyway history and agree on backup/rollback. There is no automatic migration/down-migration of the accepted demo database in Phase 1.
