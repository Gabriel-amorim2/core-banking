CREATE TABLE accounts (
                          id          UUID PRIMARY KEY,
                          user_id     UUID           NOT NULL,
                          currency    CHAR(3)        NOT NULL,
                          balance     NUMERIC(19,4)  NOT NULL DEFAULT 0,
                          status      VARCHAR(20)    NOT NULL DEFAULT 'ACTIVE',
                          created_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
                          CONSTRAINT chk_balance_non_negative CHECK (balance >= 0)
);
CREATE INDEX idx_accounts_user ON accounts (user_id);

CREATE TABLE ledger_transactions (
                                     id                 UUID PRIMARY KEY,
                                     initiator_user_id  UUID         NOT NULL,
                                     idempotency_key    VARCHAR(100) NOT NULL,
                                     request_hash       CHAR(64)     NOT NULL,
                                     type               VARCHAR(20)  NOT NULL,
                                     status             VARCHAR(20)  NOT NULL,
                                     created_at         TIMESTAMPTZ  NOT NULL,
                                     completed_at       TIMESTAMPTZ,
                                     CONSTRAINT uq_idempotency UNIQUE (initiator_user_id, idempotency_key)
);

CREATE TABLE ledger_entries (
                                id             UUID PRIMARY KEY,
                                transaction_id UUID          NOT NULL REFERENCES ledger_transactions(id),
                                account_id     UUID          NOT NULL REFERENCES accounts(id),
                                type           VARCHAR(10)   NOT NULL,
                                amount         NUMERIC(19,4) NOT NULL CHECK (amount > 0),
                                created_at     TIMESTAMPTZ   NOT NULL
);
CREATE INDEX idx_entries_tx      ON ledger_entries (transaction_id);
CREATE INDEX idx_entries_account ON ledger_entries (account_id, created_at);