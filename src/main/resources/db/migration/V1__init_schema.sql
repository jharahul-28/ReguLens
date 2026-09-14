-- V1__init_schema.sql: Initial Core Tables for ReguLens

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- App Users
CREATE TABLE IF NOT EXISTS app_user (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Roles
CREATE TABLE IF NOT EXISTS role (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role VARCHAR(255) NOT NULL UNIQUE
);

-- User Roles
CREATE TABLE IF NOT EXISTS user_role (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES role(id) ON DELETE CASCADE,
    granted_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    granted_by UUID REFERENCES app_user(id),
    revoked_at TIMESTAMP WITHOUT TIME ZONE,
    active BOOLEAN NOT NULL DEFAULT true,
    CONSTRAINT uk_user_role UNIQUE (user_id, role_id)
);

-- Policy Categories
CREATE TABLE IF NOT EXISTS policy_category (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL UNIQUE
);

-- Policy Headers
CREATE TABLE IF NOT EXISTS policy (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    owner_id UUID NOT NULL REFERENCES app_user(id),
    active BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Policy to Category Join Table
CREATE TABLE IF NOT EXISTS policy_categories (
    policy_id UUID NOT NULL REFERENCES policy(id) ON DELETE CASCADE,
    policy_category_id UUID NOT NULL REFERENCES policy_category(id) ON DELETE CASCADE,
    PRIMARY KEY (policy_id, policy_category_id)
);

-- Policy Versions
CREATE TABLE IF NOT EXISTS policy_version (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    policy_id UUID NOT NULL REFERENCES policy(id) ON DELETE CASCADE,
    version INT NOT NULL,
    content TEXT NOT NULL,
    status SMALLINT NOT NULL, -- 0: DRAFT, 1: APPROVED, 2: ACTIVE, 3: EXPIRED
    content_tsv tsvector GENERATED ALWAYS AS (to_tsvector('english', content)) STORED,
    approved_by UUID REFERENCES app_user(id),
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    effective_from TIMESTAMP WITHOUT TIME ZONE,
    effective_to TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_policy_version_policy ON policy_version(policy_id);
CREATE INDEX IF NOT EXISTS idx_policy_version_status ON policy_version(status);
