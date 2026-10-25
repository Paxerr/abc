# Flash Sale implementation plan

**Goal:** Platform-funded Flash Sale with immutable financial snapshots, safe allocation and guest visibility.
**Baseline:** 3c80558, branch feat/flash-sale. Single-agent execution expressly requested and approved; no commits/PR/deployment.
**Spec:** User Flash Sale prompt, business addendum and Phase 1 approval supplied in this conversation.
**Architecture:** Existing Booking and Order engines, domain pricing records, application ports and PostgreSQL JDBC adapters. Allocation snapshots are separate from inventory allocations. Existing Redis inventory logic is retained.

## Constraints and decisions
- Only the flash-sale worktree changes. Neither danasea nor danasea_demo may be migrated or mutated.
- Source baseline migrations inspected V1..V38 (V25/V26 absent); new migration V39.
- Admin edits DRAFT only. ENABLED terms immutable; cancellation prevents new reservations, preserving existing holds.
- Commission policy is append-only, versioned per Vendor, explicitly labelled. No production policy seeds or implicit 10% fallback for Flash Sale.
- Explicit flashSaleId selects the offer. A selected unavailable offer fails; no silent price fallback. Idempotency-Key required for promotional holds.
- PostgreSQL row locking and a range exclusion constraint prevent overselling and enabled overlap. Allocations reserve quota in BookingItem quantity units, including PRIVATE packages.
- No voucher stacking. Payment confirmation consumes a reservation; cancellation/expiry release it. Successful-payment refunds retain consumed promotion history; no new settlement claims.
- Public responses never contain policy, commission, vendor receivable, costs or budget.

## Tasks (executed sequentially)
- [x] 1. Domain money rules: tests for configured policy, subsidy ceiling, quantity, exact conservation and time boundaries; run RED then implement GREEN.
- [x] 2. V39 schema, policy/campaign persistence, immutable administration, public query and RBAC. Test migration on an isolated nonempty database.
- [x] 3. Booking integration: explicit offer selection, durable request idempotency, allocation snapshot, transactional rollback; cancellation/confirmation/expiry lifecycle.
- [x] 4. Order integration: both entry points use original-price commission snapshot, reject vouchers for promotional bookings; catalog enrichment.
- [x] 5. Real PostgreSQL/Redis and HTTP tests from Phase 1 source: time, exhaustion, concurrency, recovery, security, financial conservation and ordinary PRIVATE/SHARED regression.
- [x] 6. Self-review connected callers, record test exit codes and limitations. Leave changes uncommitted for user review.

## Review focus
Boundary equality at endsAt; customer retry with changed payload; base price changed after activation; database rollback after Redis acquisition; cancelled/expired sale while a legitimate hold is still valid.

## Progress
Worktree created from exact baseline. Root/reference changes preserved. Maven wrapper fails before launch in sandbox; using installed matching Maven with explicit existing dependency cache instead.

Completed 2026-10-25: 105 tests in 13 suites, zero failures/errors/skips; exit 0. Includes 16 real HTTP integration tests, 203 HTTP requests and complete 24-Service/33-Option regression. Package exit 0. Evidence and limitations: backend/docs/flash-sale-phase1-acceptance.md.

Review corrections: legacy expiration was not locked/transactional; now rechecked under a row lock and existing expiry events are published in the transaction. Legacy Order and settlement commission defaults were removed. Tests first demonstrated missing pricing and, later, missing settlement-policy and domain-state guards. Fixture failures were corrected by respecting slot uniqueness, valid idempotency contracts, explicit cleanup of legacy tables without FKs, and disabling only the mail health probe for the mocked outbound mail adapter. No business assertion was weakened.
