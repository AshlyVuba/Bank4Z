-- V5__create_sessions.sql
-- Server-side login sessions (B4Z-12). Reuses set_updated_at() from V1.
-- One row per login. The id is the "sid" claim carried inside that login's tokens.

CREATE TABLE sessions (
                          id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                          device_id     VARCHAR(100) NOT NULL,   -- random id generated once by the app install
                          device_name   VARCHAR(100) NOT NULL,   -- shown to the user, e.g. "Bank4Z on Android"
                          ip_address    VARCHAR(45),
                          refresh_jti   UUID NOT NULL,           -- id of the ONE refresh token currently valid for this session
                          expires_at    TIMESTAMPTZ NOT NULL,
                          last_used_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
                          revoked_at    TIMESTAMPTZ,             -- NULL = still active
                          created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
                          updated_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- "Show me my active devices"
CREATE INDEX idx_sessions_user_active ON sessions(user_id) WHERE revoked_at IS NULL;

-- "Have I seen this device for this user before?" (new-device detection)
CREATE INDEX idx_sessions_user_device ON sessions(user_id, device_id);

CREATE TRIGGER trg_sessions_updated_at
    BEFORE UPDATE ON sessions
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- New-device logins are delivered as in-app notifications.
ALTER TABLE notifications DROP CONSTRAINT IF EXISTS notifications_type_check;
ALTER TABLE notifications
    ADD CONSTRAINT notifications_type_check
        CHECK (type IN ('TRANSFER_SENT', 'TRANSFER_RECEIVED', 'FRAUD_ALERT', 'NEW_DEVICE_LOGIN'));