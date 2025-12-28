DROP TABLE IF EXISTS userservice.refresh_tokens;
DROP TABLE IF EXISTS userservice.users;

CREATE TABLE userservice.users(
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    username VARCHAR(255) UNIQUE NOT NULL,
    hashed_password TEXT NOT NULL, -- Used TEXT to avoid length issues
    is_active BOOLEAN DEFAULT TRUE, -- Used for soft delete
    created_date TIMESTAMPTZ NOT NULL DEFAULT NOW(), -- Used TIMESTAMPTZ to avoid timezone issues
    updated_date TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE userservice.refresh_tokens(
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    hashed_refresh_token TEXT NOT NULL,
    is_revoked BOOLEAN DEFAULT FALSE,
    user_id UUID NOT NULL,
    expiry_date TIMESTAMPTZ NOT NULL,
    created_date TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_date TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_user
    FOREIGN KEY (user_id)
    REFERENCES userservice.users(id)
    ON DELETE CASCADE -- if user is delete, all tokens linked to user should be deleted too
);

-- Indexing for performance
CREATE INDEX idx_users_email ON userservice.users(email);
CREATE INDEX idx_refresh_tokens_user ON userservice.refresh_tokens(user_id);