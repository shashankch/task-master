-- ====================================================================
-- V8__add_team_fk_to_tasks.sql
-- Table: tasks foreign key constraint to teams
-- ====================================================================

ALTER TABLE tasks
    ADD CONSTRAINT fk_tasks_team_id
    FOREIGN KEY (team_id)
    REFERENCES teams(id)
    ON DELETE SET NULL;
