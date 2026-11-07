/*
 * Copyright (c) 2020-2026. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot.test.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Example of logging inside scheduled operation.
 * <p>
 * Each execution of {@link #processNewRecords()} is observed by Spring (see {@code SchedulingConfig}),
 * so all log messages inside one execution share the same traceId, and different executions have different traceIds.
 */
@Slf4j
@Service
public class StorageProcessingScheduledService {

    private static final int MAX_ROWS_TO_LOG = 20;

    private final JdbcClient jdbcClient;
    private final int batchSize;
    private final Counter startsCounter;
    private final Counter noDataCounter;
    private final AtomicLong lastProcessedId = new AtomicLong();

    public StorageProcessingScheduledService(
        JdbcClient jdbcClient,
        MeterRegistry meterRegistry,
        @Value("${app.scheduled-processing.batch-size:100}") int batchSize
    ) {
        this.jdbcClient = jdbcClient;
        this.batchSize = batchSize;
        this.startsCounter = Counter.builder("storage.processing.starts")
            .description("Indicates count of starts of scheduled processNewRecords method")
            .register(meterRegistry);
        this.noDataCounter = Counter.builder("storage.processing.no.data")
            .description("Indicates count of runs of scheduled processNewRecords method when no data for processing found")
            .register(meterRegistry);
    }

    @Scheduled(
        initialDelayString = "${app.scheduled-processing.initial-delay:10s}",
        fixedDelayString = "${app.scheduled-processing.fixed-delay:10s}"
    )
    public void processNewRecords() {
        final long startTime = System.nanoTime();
        // Scheduled operations run frequently: use debug level for technical messages in order not to spam logs
        log.debug("processNewRecords is started");
        startsCounter.increment();
        try {
            final List<DataForProcess> data = getDataForProcessing();
            if (data.isEmpty()) {
                log.debug("Nothing to do at this time");
                noDataCounter.increment();
            } else {
                log.debug("Found {} rows to process", data.size());
                if (log.isTraceEnabled()) {
                    final int limit = Math.min(MAX_ROWS_TO_LOG, data.size());
                    log.trace("First {} rows to process {}", limit, data.stream().limit(limit).toList());
                }
                doSomethingUseful(data);
                lastProcessedId.set(data.getLast().rowId());
            }
        } finally {
            final long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime);
            log.debug("processNewRecords is finished. Elapsed {}ms", elapsedMillis);
        }
    }

    private void doSomethingUseful(List<DataForProcess> data) {
        final long totalSizeOfBodies = data.stream()
            .mapToLong(d -> d.body().getBytes(StandardCharsets.UTF_8).length)
            .sum();
        log.info("Successfully processed {} rows with {} bytes", data.size(), totalSizeOfBodies);
    }

    private List<DataForProcess> getDataForProcessing() {
        return jdbcClient.sql("""
                select id, message, created_at
                from otel_demo.storage
                where id > :minId
                order by id
                limit :batchSize""")
            .param("minId", lastProcessedId.get())
            .param("batchSize", batchSize)
            .query((rs, rowNum) -> new DataForProcess(
                rs.getLong("id"),
                rs.getObject("created_at", OffsetDateTime.class).toInstant(),
                rs.getString("message")
            ))
            .list();
    }

    record DataForProcess(long rowId, Instant createdAt, String body) {
    }
}
