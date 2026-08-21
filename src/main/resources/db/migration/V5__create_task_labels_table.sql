-- ====================================================================
-- V5__create_task_labels_table.sql
-- Table: task_labels
-- ====================================================================

CREATE TABLE task_labels (
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    label VARCHAR(50) NOT NULL,
    PRIMARY KEY (task_id, label)
);

CREATE INDEX idx_task_labels_label ON task_labels (label);
