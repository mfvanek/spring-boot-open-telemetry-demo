package io.github.mfvanek.spring.boot3.test.service;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaReadingService {

    private final Tracer tracer;
    private final Clock clock;
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @KafkaListener(topics = "${spring.kafka.template.default-topic}")
    public void listen(ConsumerRecord<UUID, String> message, Acknowledgment ack) {
        final Span currentSpan = tracer.currentSpan();
        final String traceId = currentSpan != null ? currentSpan.context().traceId() : "";
        log.info("Received record: {} with traceId {}", message.value(), traceId);
        jdbcTemplate.update("insert into otel_demo.storage(message, trace_id, created_at) values(:msg, :traceId, :createdAt);",
            Map.ofEntries(
                Map.entry("msg", message.value()),
                Map.entry("traceId", traceId),
                Map.entry("createdAt", LocalDateTime.now(clock))
            )
        );
        ack.acknowledge();
    }
}
