CREATE TABLE IF NOT EXISTS public.event_journal
(
    ordering           BIGSERIAL,
    persistence_id     VARCHAR(255)          NOT NULL,
    sequence_number    BIGINT                NOT NULL,
    deleted            BOOLEAN DEFAULT FALSE NOT NULL,

    writer             VARCHAR(255)          NOT NULL,
    write_timestamp    BIGINT,
    adapter_manifest   VARCHAR(255),

    event_ser_id       INTEGER               NOT NULL,
    event_ser_manifest VARCHAR(255)          NOT NULL,
    event_payload      BYTEA                 NOT NULL,

    meta_ser_id        INTEGER,
    meta_ser_manifest  VARCHAR(255),
    meta_payload       BYTEA,

    PRIMARY KEY (persistence_id, sequence_number)
);

CREATE UNIQUE INDEX event_journal_ordering_idx ON public.event_journal (ordering);

CREATE TABLE IF NOT EXISTS public.event_tag
(
    event_id        BIGINT,
    persistence_id  VARCHAR(255),
    sequence_number BIGINT,
    tag             VARCHAR(256),
    PRIMARY KEY (persistence_id, sequence_number, tag),
    CONSTRAINT fk_event_journal
        FOREIGN KEY (persistence_id, sequence_number)
            REFERENCES event_journal (persistence_id, sequence_number)
            ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS public.snapshot
(
    persistence_id        VARCHAR(255) NOT NULL,
    sequence_number       BIGINT       NOT NULL,
    created               BIGINT       NOT NULL,

    snapshot_ser_id       INTEGER      NOT NULL,
    snapshot_ser_manifest VARCHAR(255) NOT NULL,
    snapshot_payload      BYTEA        NOT NULL,

    meta_ser_id           INTEGER,
    meta_ser_manifest     VARCHAR(255),
    meta_payload          BYTEA,

    PRIMARY KEY (persistence_id, sequence_number)
);

CREATE TABLE IF NOT EXISTS public.durable_state
(
    global_offset         BIGSERIAL,
    persistence_id        VARCHAR(255) NOT NULL,
    revision              BIGINT       NOT NULL,
    state_payload         BYTEA        NOT NULL,
    state_serial_id       INTEGER      NOT NULL,
    state_serial_manifest VARCHAR(255),
    tag                   VARCHAR,
    state_timestamp       BIGINT       NOT NULL,
    PRIMARY KEY (persistence_id)
);
CREATE INDEX state_tag_idx on public.durable_state (tag);
CREATE INDEX state_global_offset_idx on public.durable_state (global_offset);