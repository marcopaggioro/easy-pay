CREATE TABLE IF NOT EXISTS transactions_history
(
    transaction_id        UUID PRIMARY KEY,
    customer_id           UUID           NOT NULL,
    recipient_customer_id UUID,
    instant               BIGINT         NOT NULL,
    amount                NUMERIC(20, 2) NOT NULL
);