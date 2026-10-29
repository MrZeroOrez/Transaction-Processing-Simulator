CREATE TABLE IF NOT EXISTS app_users(id VARCHAR(36) PRIMARY KEY,email VARCHAR(254) NOT NULL UNIQUE,password_hash VARCHAR(100) NOT NULL);
CREATE TABLE IF NOT EXISTS accounts(id VARCHAR(36) PRIMARY KEY,owner_id VARCHAR(36) NOT NULL REFERENCES app_users(id),name VARCHAR(80) NOT NULL,balance DECIMAL(19,2) NOT NULL CHECK(balance>=0),created_at VARCHAR(40) NOT NULL);
CREATE INDEX IF NOT EXISTS account_owner ON accounts(owner_id);
CREATE TABLE IF NOT EXISTS transfers(id VARCHAR(36) PRIMARY KEY,owner_id VARCHAR(36) NOT NULL REFERENCES app_users(id),source_id VARCHAR(36) NOT NULL REFERENCES accounts(id),target_id VARCHAR(36) NOT NULL REFERENCES accounts(id),amount DECIMAL(19,2) NOT NULL CHECK(amount>0),request_key VARCHAR(80) NOT NULL,note VARCHAR(140) NOT NULL,created_at VARCHAR(40) NOT NULL,UNIQUE(owner_id,request_key));
CREATE INDEX IF NOT EXISTS transfers_source ON transfers(source_id,created_at);
CREATE INDEX IF NOT EXISTS transfers_target ON transfers(target_id,created_at);
CREATE TABLE IF NOT EXISTS ledger(id VARCHAR(36) PRIMARY KEY,account_id VARCHAR(36) NOT NULL REFERENCES accounts(id),transfer_id VARCHAR(36) REFERENCES transfers(id),kind VARCHAR(20) NOT NULL,amount DECIMAL(19,2) NOT NULL,created_at VARCHAR(40) NOT NULL);
CREATE INDEX IF NOT EXISTS ledger_account ON ledger(account_id,created_at);
CREATE TABLE IF NOT EXISTS audit(id VARCHAR(36) PRIMARY KEY,owner_id VARCHAR(36) NOT NULL REFERENCES app_users(id),action VARCHAR(30) NOT NULL,resource_id VARCHAR(36) NOT NULL,created_at VARCHAR(40) NOT NULL);
