CREATE TABLE IF NOT EXISTS transactions_history
(
    transaction_id        UUID PRIMARY KEY,
    sender_customer_id    UUID           NOT NULL,
    recipient_customer_id UUID           NOT NULL,
    description           VARCHAR(500),
    instant               BIGINT         NOT NULL,
    amount                NUMERIC(20, 2) NOT NULL
);

CREATE TABLE IF NOT EXISTS users
(
    customer_id UUID PRIMARY KEY,
    first_name  VARCHAR(50),
    last_name   VARCHAR(50),
    birth_date  DATE,
    email       VARCHAR(254),
    last_edit   BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS users_balance
(
    customer_id UUID PRIMARY KEY,
    balance     NUMERIC(20, 2) NOT NULL
);

CREATE TABLE IF NOT EXISTS scheduled_operations
(
    scheduled_operation_id UUID PRIMARY KEY,
    sender_customer_id     UUID           NOT NULL,
    recipient_customer_id  UUID           NOT NULL,
    description            VARCHAR(500)   NOT NULL,
    "when"                 BIGINT         NOT NULL,
    amount                 NUMERIC(20, 2) NOT NULL,
    repeat                 VARCHAR(34),
    status                 VARCHAR(10)    NOT NULL
);