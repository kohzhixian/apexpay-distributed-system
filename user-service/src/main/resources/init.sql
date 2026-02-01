CREATE SCHEMA IF NOT EXISTS userservice;
CREATE SCHEMA IF NOT EXISTS walletservice;
CREATE SCHEMA IF NOT EXISTS paymentservice;

DROP TABLE IF EXISTS userservice.contacts;
DROP TABLE IF EXISTS userservice.refresh_tokens;
DROP TABLE IF EXISTS userservice.users;
DROP TABLE IF EXISTS walletservice.wallet_transactions;
DROP TABLE IF EXISTS walletservice.wallets;
DROP TABLE IF EXISTS paymentservice.payments;

CREATE TABLE userservice.users(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    username VARCHAR(255) UNIQUE NOT NULL,
    hashed_password TEXT NOT NULL, -- Used TEXT to avoid length issues
    is_active BOOLEAN DEFAULT TRUE, -- Used for soft delete
    created_date TIMESTAMPTZ NOT NULL DEFAULT NOW(), -- Used TIMESTAMPTZ to avoid timezone issues
    updated_date TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE userservice.refresh_tokens(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    hashed_refresh_token TEXT NOT NULL,
    is_revoked BOOLEAN DEFAULT FALSE,
    user_id UUID NOT NULL,
    family_id UUID DEFAULT gen_random_uuid(),
    consumed BOOLEAN DEFAULT FALSE, 
    expiry_date TIMESTAMPTZ NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
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

CREATE TABLE userservice.contacts(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id UUID NOT NULL,
    contact_user_id UUID NOT NULL,
    contact_wallet_id UUID NOT NULL,
    contact_email VARCHAR(255) NOT NULL,
    contact_username VARCHAR(255) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_date TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_date TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_owner_user
        FOREIGN KEY (owner_user_id)
        REFERENCES userservice.users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_contact_user
        FOREIGN KEY (contact_user_id)
        REFERENCES userservice.users(id)
        ON DELETE CASCADE,

    CONSTRAINT unique_owner_contact_wallet
        UNIQUE (owner_user_id, contact_wallet_id)
);

CREATE INDEX idx_contacts_owner_user_id ON userservice.contacts(owner_user_id);
CREATE INDEX idx_contacts_contact_wallet_id ON userservice.contacts(contact_wallet_id);


CREATE TABLE walletservice.wallets(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    wallet_name VARCHAR(50) NOT NULL,
    balance DECIMAL(15,2) NOT NULL,
    reserved_balance DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(3) NOT NULL DEFAULT 'SGD',
    version BIGINT NOT NULL DEFAULT 0,
    created_date TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_date TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_wallets_user_id ON walletservice.wallets(user_id);

CREATE TABLE walletservice.wallet_transactions(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wallet_id UUID NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    transaction_type VARCHAR(7) NOT NULL, -- CREDIT or DEBIT
    reference_id VARCHAR(255),
    reference_type VARCHAR(50),
    transaction_reference VARCHAR(20) UNIQUE, -- Human-readable reference for customer support (e.g., APX-8921-MNQ-772)
    description TEXT,
    status VARCHAR(25) NOT NULL, -- tracks the movement of balance and reserved_balance (PENDING, COMPLETED, CANCELLED)
    created_date TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_wallet
        FOREIGN KEY (wallet_id)
        REFERENCES walletservice.wallets(id)
);

CREATE INDEX idx_wallet_transactions_reference ON walletservice.wallet_transactions(transaction_reference);

CREATE TABLE paymentservice.payments(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'SGD',
    client_request_id VARCHAR(255) UNIQUE NOT NULL,
    status VARCHAR(25) NOT NULL, -- (INITIATED, PENDING, SUCCESS, FAILED, REFUNDED)
    provider VARCHAR(50),
    version BIGINT NOT NULL DEFAULT 0,
    wallet_id UUID NOT NULL,
    wallet_transaction_id UUID, -- Links to wallet transaction created during fund reservation
    failure_code VARCHAR(50),
    failure_message VARCHAR(500),
    provider_transaction_id VARCHAR(100),
    created_date TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_date TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_provider_transaction_id ON paymentservice.payments(provider_transaction_id);

-- =============================================
-- MOCK DATA FOR DEVELOPMENT
-- =============================================

-- Mock Users (password for all: "password123" - BCrypt hashed)
INSERT INTO userservice.users (id, email, username, hashed_password) VALUES
    ('11111111-1111-1111-1111-111111111111', 'kzx@gmail.com', 'kzx', '$2a$12$xopfapxJJrmyR4hZ5wgFMu/XAWx1dzYP2Pd8gXbn38QzkaHO9KpzK'),
    ('22222222-2222-2222-2222-222222222222', 'jane@example.com', 'jane_smith', '$2a$12$xopfapxJJrmyR4hZ5wgFMu/XAWx1dzYP2Pd8gXbn38QzkaHO9KpzK'),
    ('33333333-3333-3333-3333-333333333333', 'bob@example.com', 'bob_wilson', '$2a$12$xopfapxJJrmyR4hZ5wgFMu/XAWx1dzYP2Pd8gXbn38QzkaHO9KpzK'),
    ('44444444-4444-4444-4444-444444444444', 'alice@example.com', 'alice_wong', '$2a$12$xopfapxJJrmyR4hZ5wgFMu/XAWx1dzYP2Pd8gXbn38QzkaHO9KpzK');

-- Mock Wallets
INSERT INTO walletservice.wallets (id, user_id, wallet_name, balance) VALUES
    ('aaaa1111-1111-1111-1111-111111111111', '11111111-1111-1111-1111-111111111111', 'KZX Main Wallet', 1000.00),
    ('aaaa2222-2222-2222-2222-222222222222', '22222222-2222-2222-2222-222222222222', 'Jane Main Wallet', 500.00),
    ('aaaa3333-3333-3333-3333-333333333333', '33333333-3333-3333-3333-333333333333', 'Bob Main Wallet', 750.00),
    ('aaaa4444-4444-4444-4444-444444444444', '44444444-4444-4444-4444-444444444444', 'Alice Main Wallet', 1500.00);

-- Mock Contacts (kzx owns these contacts)
INSERT INTO userservice.contacts (owner_user_id, contact_user_id, contact_wallet_id, contact_email, contact_username) VALUES
    ('11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222222', 'aaaa2222-2222-2222-2222-222222222222', 'jane@example.com', 'jane_smith'),
    ('11111111-1111-1111-1111-111111111111', '33333333-3333-3333-3333-333333333333', 'aaaa3333-3333-3333-3333-333333333333', 'bob@example.com', 'bob_wilson'),
    ('11111111-1111-1111-1111-111111111111', '44444444-4444-4444-4444-444444444444', 'aaaa4444-4444-4444-4444-444444444444', 'alice@example.com', 'alice_wong');

