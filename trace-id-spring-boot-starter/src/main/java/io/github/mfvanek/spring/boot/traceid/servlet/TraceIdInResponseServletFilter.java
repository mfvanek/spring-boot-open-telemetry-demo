/*
 * Copyright (c) 2020-2026. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot.traceid.servlet;

import io.github.mfvanek.spring.boot.traceid.TraceIdResponseHeaders;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Servlet filter that adds the traceId of the current span to the response.
 */
public class TraceIdInResponseServletFilter implements Filter {

    private final Tracer tracer;

    /**
     * Creates a new filter.
     *
     * @param tracer tracer used to obtain the current span
     */
    public TraceIdInResponseServletFilter(final Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
        throws IOException, ServletException {
        final Span currentSpan = tracer.currentSpan();
        if (currentSpan != null && response instanceof HttpServletResponse httpResponse) {
            final String traceId = currentSpan.context().traceId();
            // No-op spans have an empty traceId: do not send a meaningless header
            if (!traceId.isBlank()) {
                httpResponse.addHeader(TraceIdResponseHeaders.TRACE_ID_HEADER_NAME, traceId);
            }
        }
        chain.doFilter(request, response);
    }
}
