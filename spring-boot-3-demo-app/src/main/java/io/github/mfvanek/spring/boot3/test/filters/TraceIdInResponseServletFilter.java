/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot3.test.filters;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class TraceIdInResponseServletFilter implements Filter {

    public static final String TRACE_ID_HEADER_NAME = "X-TraceId";

    private final Tracer tracer;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
        final Span currentSpan = tracer.currentSpan();
        if (currentSpan != null) {
            final HttpServletResponse resp = (HttpServletResponse) response;
            resp.addHeader(TRACE_ID_HEADER_NAME, currentSpan.context().traceId());
        }
        chain.doFilter(request, response);
    }
}
