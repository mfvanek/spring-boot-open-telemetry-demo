/*
 * Copyright (c) 2020-2026. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot.traceid.reactive;

import io.github.mfvanek.spring.boot.traceid.TraceIdResponseHeaders;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.handler.TracingObservationHandler.TracingContext;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.reactive.observation.ServerRequestObservationContext;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("PMD.CloseResource")
class TraceIdInResponseReactiveFilterTest {

    private static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final WebFilterChain COMPLETING_CHAIN = exchange -> exchange.getResponse().setComplete();

    private final TraceIdInResponseReactiveFilter filter = new TraceIdInResponseReactiveFilter();

    @Test
    void shouldAddTraceIdHeaderWhenObservationHasSpan() {
        final MockServerWebExchange exchange = newExchange();
        final ServerRequestObservationContext observationContext = attachObservationContext(exchange);
        final TracingContext tracingContext = new TracingContext();
        tracingContext.setSpan(mockSpan());
        observationContext.put(TracingContext.class, tracingContext);

        filter.filter(exchange, COMPLETING_CHAIN).block();

        assertThat(exchange.getResponse().getHeaders().getFirst(TraceIdResponseHeaders.TRACE_ID_HEADER_NAME))
            .isEqualTo(TRACE_ID);
    }

    @Test
    void shouldNotAddHeaderWithoutObservationContext() {
        final MockServerWebExchange exchange = newExchange();

        filter.filter(exchange, COMPLETING_CHAIN).block();

        assertThat(exchange.getResponse().getHeaders().containsHeader(TraceIdResponseHeaders.TRACE_ID_HEADER_NAME))
            .isFalse();
    }

    @Test
    void shouldNotAddHeaderWhenObservationHasNoTracingContext() {
        final MockServerWebExchange exchange = newExchange();
        attachObservationContext(exchange);

        filter.filter(exchange, COMPLETING_CHAIN).block();

        assertThat(exchange.getResponse().getHeaders().containsHeader(TraceIdResponseHeaders.TRACE_ID_HEADER_NAME))
            .isFalse();
    }

    @Test
    void shouldNotAddHeaderWhenTracingContextHasNoSpan() {
        final MockServerWebExchange exchange = newExchange();
        final ServerRequestObservationContext observationContext = attachObservationContext(exchange);
        observationContext.put(TracingContext.class, new TracingContext());

        filter.filter(exchange, COMPLETING_CHAIN).block();

        assertThat(exchange.getResponse().getHeaders().containsHeader(TraceIdResponseHeaders.TRACE_ID_HEADER_NAME))
            .isFalse();
    }

    @Test
    void shouldNotAddHeaderForNoopSpan() {
        final MockServerWebExchange exchange = newExchange();
        final ServerRequestObservationContext observationContext = attachObservationContext(exchange);
        final TracingContext tracingContext = new TracingContext();
        tracingContext.setSpan(Span.NOOP);
        observationContext.put(TracingContext.class, tracingContext);

        filter.filter(exchange, COMPLETING_CHAIN).block();

        assertThat(exchange.getResponse().getHeaders().containsHeader(TraceIdResponseHeaders.TRACE_ID_HEADER_NAME))
            .isFalse();
    }

    private static MockServerWebExchange newExchange() {
        return MockServerWebExchange.from(MockServerHttpRequest.get("/"));
    }

    private static ServerRequestObservationContext attachObservationContext(final ServerWebExchange exchange) {
        final ServerRequestObservationContext observationContext = new ServerRequestObservationContext(
            exchange.getRequest(), exchange.getResponse(), new HashMap<>());
        exchange.getAttributes().put(ServerRequestObservationContext.CURRENT_OBSERVATION_CONTEXT_ATTRIBUTE, observationContext);
        return observationContext;
    }

    private static Span mockSpan() {
        final TraceContext traceContext = mock(TraceContext.class);
        when(traceContext.traceId()).thenReturn(TRACE_ID);
        final Span span = mock(Span.class);
        when(span.context()).thenReturn(traceContext);
        return span;
    }
}
