--liquibase formatted sql

--changeset marina.zharinova:2025.08.31:add span column
alter table otel_demo.storage add column span_id varchar(64);

--changeset marina.zharinova:2025.08.31:comment on span_id
comment on column otel_demo.storage.span_id is 'SpanId of operation';
