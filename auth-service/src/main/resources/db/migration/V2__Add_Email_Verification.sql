CREATE TABLE IF NOT EXISTS auth.email_verification_tokens (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL,
    token        VARCHAR(10) NOT NULL,
    expiry_date  TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_email_tokens_user_id ON auth.email_verification_tokens(user_id);