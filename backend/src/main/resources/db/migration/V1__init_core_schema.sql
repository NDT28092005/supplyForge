-- SupplyForge AI — core multi-tenant schema (PostgreSQL)

CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(320) NOT NULL,
    password_hash   VARCHAR(255),
    display_name    VARCHAR(200),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_users_email UNIQUE (email)
);

CREATE TABLE workspaces (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    slug            VARCHAR(80),
    owner_user_id   BIGINT REFERENCES users (id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_workspaces_slug UNIQUE (slug)
);

CREATE INDEX idx_workspaces_owner ON workspaces (owner_user_id);

CREATE TABLE workspace_members (
    id              BIGSERIAL PRIMARY KEY,
    workspace_id    BIGINT NOT NULL REFERENCES workspaces (id) ON DELETE CASCADE,
    user_id         BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role            VARCHAR(32) NOT NULL DEFAULT 'MEMBER',
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_workspace_members_workspace_user UNIQUE (workspace_id, user_id)
);

CREATE INDEX idx_workspace_members_user ON workspace_members (user_id);

CREATE TABLE data_sources (
    id                  BIGSERIAL PRIMARY KEY,
    workspace_id        BIGINT NOT NULL REFERENCES workspaces (id) ON DELETE CASCADE,
    original_filename   VARCHAR(512) NOT NULL,
    storage_key         VARCHAR(1024) NOT NULL,
    content_type        VARCHAR(128),
    byte_size           BIGINT,
    status              VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    row_count           BIGINT,
    error_message       TEXT,
    column_mapping_json JSONB,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_data_sources_workspace ON data_sources (workspace_id);
CREATE INDEX idx_data_sources_status ON data_sources (workspace_id, status);

CREATE TABLE skus (
    id                  BIGSERIAL PRIMARY KEY,
    workspace_id        BIGINT NOT NULL REFERENCES workspaces (id) ON DELETE CASCADE,
    original_name       VARCHAR(2000) NOT NULL,
    normalized_name   VARCHAR(2000) NOT NULL,
    is_duplicate        BOOLEAN NOT NULL DEFAULT false,
    parent_sku_id       BIGINT REFERENCES skus (id) ON DELETE SET NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_skus_workspace ON skus (workspace_id);
CREATE INDEX idx_skus_workspace_normalized ON skus (workspace_id, normalized_name);
CREATE INDEX idx_skus_parent ON skus (parent_sku_id);

CREATE TABLE inventory_records (
    id              BIGSERIAL PRIMARY KEY,
    sku_id          BIGINT NOT NULL REFERENCES skus (id) ON DELETE CASCADE,
    record_date     DATE NOT NULL,
    quantity        NUMERIC(18, 4) NOT NULL DEFAULT 0,
    cost_price      NUMERIC(18, 4),
    selling_price   NUMERIC(18, 4),
    data_source_id  BIGINT REFERENCES data_sources (id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_inventory_sku_date ON inventory_records (sku_id, record_date);
CREATE INDEX idx_inventory_data_source ON inventory_records (data_source_id);

CREATE TABLE insights_and_anomalies (
    id              BIGSERIAL PRIMARY KEY,
    workspace_id    BIGINT NOT NULL REFERENCES workspaces (id) ON DELETE CASCADE,
    insight_type    VARCHAR(64) NOT NULL,
    severity        VARCHAR(32) NOT NULL DEFAULT 'INFO',
    title           VARCHAR(500) NOT NULL,
    summary         TEXT,
    payload         JSONB NOT NULL DEFAULT '{}',
    sku_id          BIGINT REFERENCES skus (id) ON DELETE SET NULL,
    computed_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    valid_until     TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_insights_workspace_type ON insights_and_anomalies (workspace_id, insight_type);
CREATE INDEX idx_insights_workspace_computed ON insights_and_anomalies (workspace_id, computed_at DESC);
