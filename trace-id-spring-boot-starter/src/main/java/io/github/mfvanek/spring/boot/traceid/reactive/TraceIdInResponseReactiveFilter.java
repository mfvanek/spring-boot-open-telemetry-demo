/*
 * Copyright (c) 2020-2026. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot.traceid.reactive;

import io.github.mfvanek.spring.boot.traceid.TraceIdResponseHeaders;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.handler.TracingObservationHandler.TracingContext;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.reactive.observation.ServerRequestObservationContext;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Reactive web filter that adds the traceId of the current server observation to the response.
 */
public class TraceIdInResponseReactiveFilter implements WebFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TraceIdInResponseReactiveFilter.class);

    @NonNull
    @Override
    public Mono<Void> filter(@NonNull final ServerWebExchange exchange, @NonNull final WebFilterChain chain) {
        exchange.getResponse().beforeCommit(() -> {
            final Span currentSpan = extractCurrentSpan(exchange);
            if (currentSpan != null) {
                final String traceId = currentSpan.context().traceId();
                // No-op spans have an empty traceId: do not send a meaningless header
                if (!traceId.isBlank()) {
                    exchange.getResponse().getHeaders().add(TraceIdResponseHeaders.TRACE_ID_HEADER_NAME, traceId);
                    LOGGER.debug("Added TraceId: {} to the response", traceId);
                }
            }
            return Mono.empty();
        });
        return chain.filter(exchange);
    }

    @SuppressWarnings("PMD.CloseResource")
    @Nullable
    private static Span extractCurrentSpan(final ServerWebExchange exchange) {
        final ServerRequestObservationContext observationContext = exchange.getAttribute(ServerRequestObservationContext.CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE);
        if (observationContext != null) {
            final TracingContext traceContext = observationContext.get(TracingContext.class);
            if (traceContext != null) {
                return traceContext.getSpan();
            }
        }
        return null;
    }
}
