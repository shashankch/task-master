-- ====================================================================
-- V9__create_task_comments_table.sql
-- Table: task_comments (Threaded Hierarchical Comments)
-- ====================================================================

CREATE TABLE task_comments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    author_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    parent_id UUID REFERENCES task_comments(id) ON DELETE CASCADE,
    content TEXT NOT NULL,
    deleted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_task_comments_task_id ON task_comments (task_id);
CREATE INDEX idx_task_comments_author_id ON task_comments (author_id);
CREATE INDEX idx_task_comments_parent_id ON task_comments (parent_id);
