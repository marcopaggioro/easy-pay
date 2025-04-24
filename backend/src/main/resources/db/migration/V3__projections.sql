CREATE TABLE IF NOT EXISTS users
(
    customer_id        UUID PRIMARY KEY,
    first_name         VARCHAR(50)  NOT NULL,
    last_name          VARCHAR(50)  NOT NULL,
    birth_date         DATE         NOT NULL,
    email              VARCHAR(254) NOT NULL UNIQUE,
    encrypted_password VARCHAR(128) NOT NULL,
    last_edit          BIGINT       NOT NULL
);

CREATE TABLE IF NOT EXISTS users_payment_cards
(
    customer_id UUID REFERENCES users (customer_id),
    card_id     INT          NOT NULL,
    full_name   VARCHAR(101) NOT NULL,
    card_number VARCHAR(19)  NOT NULL,
    expiration  DATE NOT NULL,
    PRIMARY KEY (customer_id, card_id)
);

CREATE TABLE IF NOT EXISTS users_balance
(
    customer_id UUID PRIMARY KEY REFERENCES users (customer_id),
    balance     NUMERIC(20, 2) NOT NULL
);

CREATE TABLE IF NOT EXISTS transactions_history
(
    transaction_id        UUID PRIMARY KEY,
    sender_customer_id    UUID           NOT NULL REFERENCES users (customer_id),
    recipient_customer_id UUID           NOT NULL REFERENCES users (customer_id),
    description           VARCHAR(140),
    instant               BIGINT         NOT NULL,
    amount                NUMERIC(20, 2) NOT NULL
);

CREATE TABLE IF NOT EXISTS scheduled_operations
(
    scheduled_operation_id UUID PRIMARY KEY,
    sender_customer_id     UUID           NOT NULL REFERENCES users (customer_id),
    recipient_customer_id  UUID           NOT NULL REFERENCES users (customer_id),
    description            VARCHAR(140)   NOT NULL,
    "when"                 BIGINT         NOT NULL,
    amount                 NUMERIC(20, 2) NOT NULL,
    repeat                 VARCHAR(20),
    status                 VARCHAR(10)    NOT NULL
);