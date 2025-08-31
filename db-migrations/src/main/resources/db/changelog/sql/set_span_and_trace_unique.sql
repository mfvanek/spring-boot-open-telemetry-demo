--liquibase formatted sql

--changeset ivan.vakhrushev:2025.08.31:remove unique from trace_id
alter table otel_demo.storage drop constraint storage_trace_id_key;

--changeset marina.zharinova:2025.08.31:add constraint on trace_id with span_id
alter table otel_demo.storage add constraint trace_span_unique unique(trace_id, span_id);
