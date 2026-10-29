CREATE TABLE notifications (
                               id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                               type        VARCHAR(30) NOT NULL
                                   CHECK (type IN ('TRANSFER_SENT', 'TRANSFER_RECEIVED')),
                               message     VARCHAR(255) NOT NULL,
                               is_read     BOOLEAN NOT NULL DEFAULT FALSE,
                               created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
                               updated_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Fast "newest first" listing per user
CREATE INDEX idx_notifications_user_created ON notifications(user_id, created_at DESC);

-- Tiny partial index: the unread badge count only ever looks at unread rows
CREATE INDEX idx_notifications_user_unread ON notifications(user_id) WHERE is_read = FALSE;

CREATE TRIGGER trg_notifications_updated_at
    BEFORE UPDATE ON notifications
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();