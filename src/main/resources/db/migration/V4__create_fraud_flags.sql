-- V4__create_fraud_flags.sql
-- Fraud detection (B4Z-11). Reuses set_updated_at() from V1.

CREATE TABLE fraud_flags (
                             id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                             transaction_id  UUID NOT NULL REFERENCES transactions(id) ON DELETE CASCADE,
                             rule_code       VARCHAR(30) NOT NULL
                                 CHECK (rule_code IN ('LARGE_TRANSFER', 'RAPID_TRANSFERS')),
                             reason          VARCHAR(255) NOT NULL,
                             risk_level      VARCHAR(10) NOT NULL
                                 CHECK (risk_level IN ('MEDIUM', 'HIGH')),
                             action_taken    VARCHAR(10) NOT NULL
                                 CHECK (action_taken IN ('ALLOWED', 'BLOCKED')),
                             resolved        BOOLEAN NOT NULL DEFAULT FALSE,
                             created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
                             updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_fraud_flags_transaction_id ON fraud_flags(transaction_id);
CREATE INDEX idx_fraud_flags_created_at ON fraud_flags(created_at DESC);

-- Admins mostly look at what still needs review
CREATE INDEX idx_fraud_flags_unresolved ON fraud_flags(created_at DESC) WHERE resolved = FALSE;

CREATE TRIGGER trg_fraud_flags_updated_at
    BEFORE UPDATE ON fraud_flags
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- The rapid-transfer rule counts "recent completed transfers out of this
-- account" on every transfer, so give that query a matching index.
CREATE INDEX idx_transactions_account_type_created
    ON transactions(account_id, type, created_at DESC);

-- Fraud alerts are delivered as in-app notifications, so widen the allowed types.
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_type_check;
ALTER TABLE notifications
    ADD CONSTRAINT notifications_type_check
        CHECK (type IN ('TRANSFER_SENT', 'TRANSFER_RECEIVED', 'FRAUD_ALERT'));