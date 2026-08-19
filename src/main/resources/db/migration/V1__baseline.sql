-- ====================================================================
-- V1__baseline.sql
-- TaskMaster Database Migration Baseline
-- Schema tables are incrementally provisioned in versioned migrations
-- ====================================================================

-- Ensure uuid-ossp / pgcrypto is available if needed
CREATE EXTENSION IF NOT EXISTS "pgcrypto";
