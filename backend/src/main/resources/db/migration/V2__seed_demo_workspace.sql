-- Workspace demo cho MVP UI (workspace_id thường = 1 trên DB trống)
INSERT INTO users (email, display_name, created_at, updated_at)
VALUES ('demo@supplyforge.local', 'Demo Seller', now(), now())
ON CONFLICT (email) DO NOTHING;

INSERT INTO workspaces (name, slug, owner_user_id, created_at, updated_at)
SELECT 'Cửa hàng Demo', 'demo', u.id, now(), now()
FROM users u
WHERE u.email = 'demo@supplyforge.local'
  AND NOT EXISTS (SELECT 1 FROM workspaces w WHERE w.slug = 'demo')
LIMIT 1;
