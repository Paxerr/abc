# Flash Sale Phase 1 acceptance — 25/10/2026

**FINAL REVIEW IN PROGRESS:** the 105-test run passed; a connected report-ledger funding correction is being verified before final acceptance.

Code and tests are uncommitted in `D:\Nam4\PBL6_HTTT\.worktrees\flash-sale`, branch `feat/flash-sale`, based on `3c805582a2756906f9f07c5e9ad90c4c3c54f4b8`. Implementation and review were performed sequentially by one agent, as requested; no independent subagent review is claimed.

## Delivered behavior

1. **Architecture:** domain pricing/state rules, application service and ports, JDBC persistence adapter sharing Booking's JPA transaction. Existing Booking/Order engines and Redis Lua inventory are reused.
2. **Commission:** append-only `vendor_commission_policies`, versioned per Vendor with required provenance and validated rate/ratio/cost/margin. No seeded/default policy. Missing policy blocks activation. Normal Order/voucher preview resolve configured policies; settlement requires a stored or explicitly supplied rate. Legacy missing snapshots fail rather than inventing 10%.
3. **Admin APIs:** eligible Options, preview, DRAFT create/read/update/delete, activate/cancel, policy create/history; ADMIN RBAC and audit logging. [API details and examples](flash-sale-phase1.md#api-inventory).
4. **Public APIs:** active/detail without JWT and additive catalog/detail `flashSales` data. No commission, cost, margin or budget in public responses. DRAFT/future/expired/cancelled/stale-price offers are hidden; sold-out offers are absent from active list.
5. **Booking:** explicit flashSaleId selection, verified Option price, immutable allocation/financial snapshot, PostgreSQL quota/budget reservation, existing Redis inventory and durable customer/key/payload idempotency. No client-supplied final-price authority or silent fallback to a higher price.
6. **Order:** both creation paths retain original unit/subtotal/commission basis, snapshot commission and Vendor receivable, with platform subsidy separated from customer payable. Voucher stacking is rejected. HOLD is a reservation, not realized revenue or paid-out Vendor money.
7. **Flyway:** V39 only; V1–V38 unchanged. No account, Vendor, Service or policy seed in V39. Schema includes range exclusion, usage guards, immutable terms/policies/snapshots and atomic allocation lifecycle triggers.
8. **Lifecycle/concurrency:** ordered campaign locks, same-key advisory locks and real concurrent HTTP requests. Confirmed Booking consumes once; cancellation/expiry releases once. Expiry locks/rechecks the Booking and publishes the existing event in its transaction. Valid Hold confirmed after sale end retains its price. Refund/settlement use customer cash and original commission basis; consumed promotion history is retained after a paid refund.

## Automated and HTTP evidence

Final test command selector is documented in [the runbook](flash-sale-phase1.md#verification-environment-and-evidence).

| Check | Final result |
|---|---|
| Selected automated suites | **105 tests / 13 suites; 0 failures, 0 errors, 0 skipped; exit 0** |
| Flash Sale pricing/domain suite | 8 tests, all passed |
| Flash Sale real HTTP integration suite | 16 tests, all passed |
| Real TCP HTTP requests | **203**, including 67 Guest requests |
| HTTP statuses observed | 76×200, 61×201, 1×202, 36×204, 2×401, 5×403, 1×404, 2×409, 17×422, 2×500 |
| Expected 500 responses | Exactly two injected recovery cases: PostgreSQL constraint/commit failure and ambiguous Redis response loss |
| Build/package | **exit 0**, executable Spring Boot JAR generated |
| Diff whitespace check | `git diff --check`, exit 0 |

Integration coverage includes immediate/future activation, arbitrary duration, exclusive endsAt, expiry, quota/budget exhaustion, enabled overlap, immutable active terms, missing commission, stale Option price, public privacy, Admin RBAC, Guest denial for Hold, ownership, snapshot persistence, both Order paths, no Voucher stacking, policy version freeze, cancellation/expiry retries, concurrent allocation, concurrent same-key retries and PostgreSQL/Redis compensation.

The payment-after-sale-end scenario uses the existing **test-profile** callback with a valid HMAC signature and a controlled PENDING payment fixture. It exercises the real payment-success/Booking-confirmation path and duplicate callback handling. The refund HTTP request returns **470000**, not the **500000** original price; signed refund processing persists PROCESSED while allocation history remains CONSUMED. This is local lifecycle verification, not proof of an external transfer.

## Regression of original catalog

The source catalog was read from `danasea_demo` in a verified READ ONLY, REPEATABLE READ transaction and copied into the isolated DB. Only public catalog fields and synthetic local authentication fixtures were used; no passwords, bank details or safety attestations were copied.

- Exact original UUID-set comparison: **24/24**, no missing/extra Service.
- Public details: **24/24 HTTP 200** on the Phase 1 runtime.
- All **33 Options** had a bookable future slot and successful ordinary Hold at their own Option price.
- All **33 regression Holds** were cancelled through HTTP successfully.
- Both PRIVATE/PER_PACKAGE and SHARED/PER_PERSON were exercised; normal flow generated no Flash Sale allocations.

Fixture resets between test methods mean the final test DB contents are the last scenario, not a permanent 24-Service imported demo. The per-request evidence preserves the completed regression.

## Environment and provenance

HTTP tests ran the freshly compiled Phase 1 application at RANDOM_PORT **51090**, not the pre-existing `localhost:8081` binary. The test server closes when JUnit finishes.

| Resource | Final verification |
|---|---|
| Writable PostgreSQL | `danasea_flashsale_test/public`, Flyway **V39**, success |
| Writable Redis | Existing `danasea-demo-redis`, port 6380, **logical DB 15** |
| Protected demo PostgreSQL | `danasea_demo/public`, still **V38**; 12 users, 9 Vendors, 24 PUBLISHED Services, 33 Options, 41 Slots; **0 Flash Sale tables** |
| Protected main PostgreSQL | `danasea/public`, still **V24**; 24 Services, 0 PUBLISHED |
| Test DB financial reconciliation | **0 invalid snapshots, 0 overdrawn campaigns** |
| Docker | Existing container set unchanged; no new container/volume |

JAR: `backend/target/backend-0.0.1-SNAPSHOT.jar`, 89,299,530 bytes.

SHA256: `D672630FBD64C535A4AE4365E725407E8E3391A20C90778CF533159CE0C8BA45`.

Evidence artifacts (local generated build output):

- [`target/flash-sale-http-evidence.jsonl`](../target/flash-sale-http-evidence.jsonl): provenance and method/path/role/status/response, without JWTs.
- [`target/flash-sale-verification.json`](../target/flash-sale-verification.json): machine-readable test summary.
- [`target/flash-sale-tests.log`](../target/flash-sale-tests.log), [`target/surefire-reports/`](../target/surefire-reports/): execution/assertion evidence.
- [`target/flash-sale-package.log`](../target/flash-sale-package.log): packaging execution.
- `target/pricing-red.log` and `target/final-review-red.log`: observed failing tests before their implementations/fixes.

## Git review and remaining boundaries

Main changes: new `modules/flashsale`, `FlashSaleBookingPort`, `ExpireBookingHoldUseCase`, V39 and Flash Sale tests; existing Booking DTO/controller/Hold flow, both Order creation paths, voucher preview, commission adapter/port, catalog response enrichment and settlement missing-rate handling. Existing expiry unit tests now check job delegation; transactional expiry and confirmation-race protection are covered by real integration scenarios.

Review with `git diff` and `git ls-files --others --exclude-standard` inside the Flash Sale worktree; new files remain untracked until the user decides to stage them. Root HEAD remains `09b9106` with its original local changes. Reference worktree remains `3c80558` with its original untracked publication test. No commit/push/merge/PR/deployment was performed.

No critical blocker remains for this isolated local acceptance. Limits:

- No external payment gateway transfer or real Vendor payout was exercised. Rabbit/email delivery are mocked in HTTP tests to prevent outbound messages.
- This is a targeted 105-test verification, not a claim that the entire repository's test suite or every possible process-crash scenario was run. Persistent Redis outages rely on existing hold TTL plus recovery after availability returns; this is not a distributed atomic transaction.
- No official commission agreement was invented. A target environment must have explicit labelled policies; legacy settlement rows with missing commission snapshots require review.
- Public sample document metadata does not establish authentic safety/legal certification. This task changed no approval policy or accepted demo data.
- The accepted demo DB/runtime has not received V39 or the new code. Applying there remains a separate Phase 2 decision, including extension/backup/compatibility review.

**GATE = PHASE_1_COMPLETE; PHASE_2_NOT_AUTHORIZED.**
