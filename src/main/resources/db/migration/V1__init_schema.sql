
-- users

CREATE TABLE users (
    id             BIGSERIAL PRIMARY KEY,
    username       VARCHAR(255) NOT NULL,
    email          VARCHAR(255) NOT NULL,
    password       VARCHAR(255) NOT NULL,
    role           VARCHAR(20)  NOT NULL,
    storage_quota  BIGINT       NOT NULL,
    used_storage   BIGINT       NOT NULL,
    created_at     TIMESTAMP    NOT NULL,

    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email)
);


-- folders

CREATE TABLE folders (
    id                BIGSERIAL PRIMARY KEY,
    name              VARCHAR(255) NOT NULL,
    owner_id          BIGINT       NOT NULL,
    parent_folder_id  BIGINT,
    created_at        TIMESTAMP    NOT NULL,

    CONSTRAINT fk_folders_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT fk_folders_parent FOREIGN KEY (parent_folder_id) REFERENCES folders (id)
);

CREATE INDEX idx_folders_owner_id ON folders (owner_id);
CREATE INDEX idx_folders_parent_folder_id ON folders (parent_folder_id);

-- file_metadata

CREATE TABLE file_metadata (
    id                  BIGSERIAL PRIMARY KEY,
    original_file_name  VARCHAR(255) NOT NULL,
    content_type        VARCHAR(255) NOT NULL,
    size                BIGINT       NOT NULL,
    storage_key         VARCHAR(255) NOT NULL,
    owner_id            BIGINT       NOT NULL,
    parent_folder_id    BIGINT,
    deleted             BOOLEAN      NOT NULL DEFAULT FALSE,
    deleted_at          TIMESTAMP,
    created_at          TIMESTAMP    NOT NULL,
    updated_at          TIMESTAMP    NOT NULL,

    CONSTRAINT uq_file_metadata_storage_key UNIQUE (storage_key),
    CONSTRAINT fk_file_metadata_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT fk_file_metadata_parent_folder FOREIGN KEY (parent_folder_id) REFERENCES folders (id)
);

CREATE INDEX idx_file_metadata_owner_id ON file_metadata (owner_id);
CREATE INDEX idx_file_metadata_parent_folder_id ON file_metadata (parent_folder_id);

CREATE INDEX idx_file_metadata_owner_deleted ON file_metadata (owner_id, deleted);
CREATE INDEX idx_file_metadata_deleted_deleted_at ON file_metadata (deleted, deleted_at);


-- share_links

CREATE TABLE share_links (
    id             BIGSERIAL PRIMARY KEY,
    token          VARCHAR(255) NOT NULL,
    file_id        BIGINT       NOT NULL,
    created_by_id  BIGINT       NOT NULL,
    permission     VARCHAR(20)  NOT NULL,
    expires_at     TIMESTAMP    NOT NULL,
    revoked        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP    NOT NULL,

    CONSTRAINT uq_share_links_token UNIQUE (token),
    CONSTRAINT fk_share_links_file FOREIGN KEY (file_id) REFERENCES file_metadata (id),
    CONSTRAINT fk_share_links_created_by FOREIGN KEY (created_by_id) REFERENCES users (id)
);

CREATE INDEX idx_share_links_file_id ON share_links (file_id);
CREATE INDEX idx_share_links_created_by_id ON share_links (created_by_id);


-- audit_logs
CREATE TABLE audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT,
    action      VARCHAR(30)  NOT NULL,
    target_id   BIGINT,
    ip_address  VARCHAR(45),
    success     BOOLEAN      NOT NULL DEFAULT TRUE,
    detail      VARCHAR(500),
    timestamp   TIMESTAMP    NOT NULL


);

CREATE INDEX idx_audit_logs_user_id ON audit_logs (user_id);

CREATE INDEX idx_audit_logs_timestamp ON audit_logs (timestamp DESC);


-- refresh_tokens

CREATE TABLE refresh_tokens (
    id            BIGSERIAL PRIMARY KEY,
    token_hash    VARCHAR(255) NOT NULL,
    token_family  VARCHAR(255) NOT NULL,
    user_id       BIGINT       NOT NULL,
    expires_at    TIMESTAMP    NOT NULL,
    revoked       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP    NOT NULL,

    CONSTRAINT uq_refresh_tokens_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);

CREATE INDEX idx_refresh_tokens_token_family ON refresh_tokens (token_family);
