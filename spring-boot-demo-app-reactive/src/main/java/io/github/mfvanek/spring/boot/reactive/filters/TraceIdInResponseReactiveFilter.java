/*
 * Copyright (c) 2020-2026. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot.reactive.filters;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.handler.TracingObservationHandler.TracingContext;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.reactive.observation.ServerRequestObservationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class TraceIdInResponseReactiveFilter implements WebFilter {

    public static final String TRACE_ID_HEADER_NAME = "X-TraceId";
    private static final Logger LOGGER = LoggerFactory.getLogger(TraceIdInResponseReactiveFilter.class);

    @NonNull
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        exchange.getResponse().beforeCommit(() -> {
            final Span currentSpan = extractCurrentSpan(exchange);
            if (currentSpan != null) {
                final String traceId = currentSpan.context().traceId();
                exchange.getResponse().getHeaders().add(TRACE_ID_HEADER_NAME, traceId);
                LOGGER.info("Added TraceId: {} to the response", traceId);
            }
            return Mono.empty();
        });
        return chain.filter(exchange);
    }

    @SuppressWarnings("PMD.CloseResource")
    @Nullable
    private static Span extractCurrentSpan(ServerWebExchange exchange) {
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
