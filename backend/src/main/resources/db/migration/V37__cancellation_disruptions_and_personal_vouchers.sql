ALTER TABLE sub_orders ADD COLUMN cancellation_policy_version VARCHAR(40) NOT NULL DEFAULT 'LEGACY_TIERED_V1';
UPDATE sub_orders s SET cancellation_policy_version = 'STANDARD_24H_V2'
FROM master_orders o WHERE s.master_order_id = o.id AND o.status = 'PENDING_PAYMENT';
ALTER TABLE sub_orders ALTER COLUMN cancellation_policy_version SET DEFAULT 'STANDARD_24H_V2';
ALTER TABLE sub_orders ADD CONSTRAINT chk_cancellation_policy_version
    CHECK (cancellation_policy_version IN ('LEGACY_TIERED_V1', 'STANDARD_24H_V2'));

ALTER TABLE refunds DROP CONSTRAINT IF EXISTS chk_refunds_reason;
ALTER TABLE refunds ADD CONSTRAINT chk_refunds_reason CHECK (reason IN
    ('CUSTOMER_REQUEST','CUSTOMER_CANCEL','WEATHER','VENDOR_FAULT','SYSTEM_FAULT','ADMIN_OVERRIDE','DISPUTE','COMPENSATION'));

ALTER TABLE discount_codes ADD COLUMN recipient_customer_id UUID REFERENCES users(id);
ALTER TABLE discount_codes ADD COLUMN replacement_sub_order_id UUID REFERENCES sub_orders(id);
CREATE UNIQUE INDEX uq_replacement_voucher_sub_order ON discount_codes(replacement_sub_order_id)
    WHERE replacement_sub_order_id IS NOT NULL;
CREATE INDEX idx_discount_recipient ON discount_codes(recipient_customer_id);
ALTER TABLE discount_codes ADD CONSTRAINT chk_replacement_voucher CHECK
    (replacement_sub_order_id IS NULL OR
        (recipient_customer_id IS NOT NULL AND max_uses IS NOT NULL AND max_uses_per_user IS NOT NULL
         AND max_uses = 1 AND max_uses_per_user = 1
         AND discount_type = 'FIXED' AND scope = 'PLATFORM' AND sponsor_type = 'PLATFORM'));

CREATE TABLE sub_order_disruptions (
    id UUID PRIMARY KEY,
    sub_order_id UUID NOT NULL REFERENCES sub_orders(id),
    original_slot_id UUID NOT NULL REFERENCES service_slots(id),
    original_departure_at TIMESTAMPTZ NOT NULL,
    decision_deadline TIMESTAMPTZ NOT NULL,
    reason VARCHAR(30) NOT NULL CHECK (reason IN ('WEATHER','VENDOR_FAULT','SYSTEM_FAULT')),
    details TEXT NOT NULL,
    source_key VARCHAR(150) NOT NULL,
    verified_by UUID REFERENCES users(id),
    status VARCHAR(30) NOT NULL CHECK (status IN ('AWAITING_DECISION','RESCHEDULED','REFUND_REQUESTED','CLOSED')),
    refund_id UUID REFERENCES refunds(id),
    notification_sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_disruption_source UNIQUE (sub_order_id, source_key),
    CONSTRAINT chk_disruption_deadline CHECK (decision_deadline = original_departure_at - INTERVAL '12 hours')
);
CREATE UNIQUE INDEX uq_active_disruption ON sub_order_disruptions(sub_order_id)
    WHERE status = 'AWAITING_DECISION';
CREATE INDEX idx_disruption_due ON sub_order_disruptions(status, decision_deadline);

-- Existing unresolved proposals also enter the decision workflow, without touching paid history.
INSERT INTO sub_order_disruptions
    (id, sub_order_id, original_slot_id, original_departure_at, decision_deadline, reason, details, source_key, status)
SELECT DISTINCT ON (s.id) p.id, s.id, s.slot_id,
    (sl.date + sl.start_time) AT TIME ZONE 'Asia/Ho_Chi_Minh',
    ((sl.date + sl.start_time) AT TIME ZONE 'Asia/Ho_Chi_Minh') - INTERVAL '12 hours',
    CASE WHEN p.reason_type = 'WEATHER' THEN 'WEATHER' ELSE 'VENDOR_FAULT' END,
    p.reason, 'proposal-' || p.id::text, 'AWAITING_DECISION'
FROM sub_order_reschedule_proposals p
JOIN sub_orders s ON s.id = p.sub_order_id
JOIN master_orders o ON o.id = s.master_order_id
JOIN service_slots sl ON sl.id = s.slot_id
WHERE p.status = 'PENDING' AND s.status = 'CONFIRMED' AND s.checked_in_at IS NULL
    AND o.payment_status = 'PAID'
ORDER BY s.id, p.created_at DESC;
UPDATE sub_order_reschedule_proposals p SET expires_at = LEAST(p.expires_at, d.decision_deadline)
FROM sub_order_disruptions d WHERE d.sub_order_id = p.sub_order_id AND p.status = 'PENDING';
