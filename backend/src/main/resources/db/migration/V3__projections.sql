CREATE TABLE IF NOT EXISTS transactions_history
(
    transaction_id        UUID PRIMARY KEY,
    sender_customer_id    UUID NOT NULL,
    recipient_customer_id UUID           NOT NULL,
    instant               BIGINT         NOT NULL,
    amount                NUMERIC(20, 2) NOT NULL
);

CREATE TABLE IF NOT EXISTS users
(
    customer_id UUID PRIMARY KEY,
    first_name  VARCHAR(50),
    last_name   VARCHAR(50),
    birth_date  DATE,
    email       VARCHAR(254)
);