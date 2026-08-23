-- ====================================================================
-- V11__create_notifications_table.sql
-- Table: notifications (Real-time and persistent notification center)
-- ====================================================================

CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    recipient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT,
    metadata TEXT,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    read_at TIMESTAMPTZ,
    CONSTRAINT chk_notification_type CHECK (type IN ('TASK_ASSIGNED', 'COMMENT_ADDED', 'TEAM_INVITE', 'TASK_UPDATED', 'MENTION'))
);

CREATE INDEX idx_notifications_recipient_id ON notifications (recipient_id);
CREATE INDEX idx_notifications_is_read ON notifications (recipient_id, is_read);
CREATE INDEX idx_notifications_created_at ON notifications (recipient_id, created_at DESC);
