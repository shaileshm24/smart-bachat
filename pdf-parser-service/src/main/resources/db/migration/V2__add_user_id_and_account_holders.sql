-- Migration: Add user_id column to existing tables and create account_holders table
-- Version: V2
-- Description: Adds user_id for multi-tenancy support and creates account_holders table for Setu profile data

-- Add user_id column to bank_accounts table
ALTER TABLE bank_accounts ADD COLUMN IF NOT EXISTS user_id UUID;
CREATE INDEX IF NOT EXISTS idx_bank_accounts_user_id ON bank_accounts(user_id);

-- Add user_id column to transactions table
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS user_id UUID;
CREATE INDEX IF NOT EXISTS idx_transactions_user_id ON transactions(user_id);

-- Add user_id column to sync_history table
ALTER TABLE sync_history ADD COLUMN IF NOT EXISTS user_id UUID;
CREATE INDEX IF NOT EXISTS idx_sync_history_user_id ON sync_history(user_id);

-- Add user_id column to statement_metadata table
ALTER TABLE statement_metadata ADD COLUMN IF NOT EXISTS user_id UUID;
CREATE INDEX IF NOT EXISTS idx_statement_metadata_user_id ON statement_metadata(user_id);

-- Create account_holders table for storing Setu profile holder data
CREATE TABLE IF NOT EXISTS account_holders (
    id UUID PRIMARY KEY,
    bank_account_id UUID NOT NULL,
    user_id UUID,
    name VARCHAR(255),
    dob VARCHAR(50),
    mobile VARCHAR(50),
    email VARCHAR(255),
    pan VARCHAR(20),
    address TEXT,
    nominee VARCHAR(255),
    ckyc_compliance BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_account_holders_bank_account FOREIGN KEY (bank_account_id) REFERENCES bank_accounts(id) ON DELETE CASCADE
);

-- Create indexes for account_holders table
CREATE INDEX IF NOT EXISTS idx_account_holders_bank_account_id ON account_holders(bank_account_id);
CREATE INDEX IF NOT EXISTS idx_account_holders_user_id ON account_holders(user_id);
CREATE INDEX IF NOT EXISTS idx_account_holders_pan ON account_holders(pan);

-- Comment on columns for documentation
COMMENT ON COLUMN bank_accounts.user_id IS 'User ID from UAM service for multi-tenancy';
COMMENT ON COLUMN transactions.user_id IS 'User ID from UAM service for multi-tenancy';
COMMENT ON COLUMN sync_history.user_id IS 'User ID from UAM service for multi-tenancy';
COMMENT ON COLUMN statement_metadata.user_id IS 'User ID from UAM service for multi-tenancy';
COMMENT ON TABLE account_holders IS 'Stores account holder profile data from Setu Account Aggregator';

