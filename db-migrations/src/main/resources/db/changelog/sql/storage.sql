--liquibase formatted sql

--changeset ivan.vakhrushev:2024.08.04:storage.table
create table if not exists otel_demo.storage
(
    id bigint generated always as identity,
    message text not null,
    trace_id text not null unique,
    created_at timestamptz not null
);

--changeset ivan.vakhrushev:2024.08.04:storage.comments.on.table.and.columns
comment on table otel_demo.storage is 'Information about messages from Kafka';
comment on column otel_demo.storage.id is 'Unique identifier of the record in the current table';
comment on column otel_demo.storage.message is 'Message from Kafka';
comment on column otel_demo.storage.trace_id is 'Unique traceId of operation';
comment on column otel_demo.storage.created_at is 'Date and time of operation';
