--liquibase formatted sql

--changeset ivan.vakhrushev:2024.08.04:create.schema
create schema if not exists otel_demo;
