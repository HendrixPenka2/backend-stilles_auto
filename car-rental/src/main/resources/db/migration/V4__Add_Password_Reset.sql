CREATE TABLE IF NOT EXISTS auth.password_reset_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token TEXT NOT NULL UNIQUE,
    expiry_date TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_reset_user_id ON auth.password_reset_tokens(user_id);
CREATE INDEX idx_reset_token ON auth.password_reset_tokens(token);