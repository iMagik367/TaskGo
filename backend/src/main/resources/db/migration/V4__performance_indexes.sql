-- Migração para índices de performance
CREATE INDEX IF NOT EXISTS idx_products_category_active ON products(category, active);
CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_user_used ON password_reset_tokens(user_id, used);
