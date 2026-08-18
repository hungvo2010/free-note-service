CREATE TABLE IF NOT EXISTS app_user (
    user_id    VARCHAR(64)  PRIMARY KEY,
    user_name  VARCHAR(255) NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS tool (
    tool_id    VARCHAR(64) PRIMARY KEY,
    tool_name  VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS draft (
    draft_id     VARCHAR(64)  PRIMARY KEY,
    draft_name   VARCHAR(255) NOT NULL DEFAULT 'Untitled',
    creator_id   VARCHAR(64)  REFERENCES app_user(user_id) ON DELETE SET NULL,
    action_count BIGINT       NOT NULL DEFAULT 0,
    created_at   TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_draft_creator ON draft(creator_id);

CREATE TABLE IF NOT EXISTS draft_action (
    draft_id    VARCHAR(64) NOT NULL REFERENCES draft(draft_id) ON DELETE CASCADE,
    seq         BIGINT      NOT NULL,
    shapes_json JSONB       NOT NULL,
    created_at  TIMESTAMP   NOT NULL DEFAULT now(),
    PRIMARY KEY (draft_id, seq)
);

INSERT INTO app_user (user_id, user_name) VALUES
    ('dev-user', 'dev')
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO tool (tool_id, tool_name) VALUES
    ('rect',   'rectangle'),
    ('circ',   'circle'),
    ('line',   'line'),
    ('text',   'text'),
    ('pen',    'pen'),
    ('eraser', 'eraser')
ON CONFLICT (tool_id) DO NOTHING;
