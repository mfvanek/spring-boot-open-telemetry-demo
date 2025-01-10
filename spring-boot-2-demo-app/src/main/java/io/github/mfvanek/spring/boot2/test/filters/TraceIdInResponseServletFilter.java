/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot2.test.filters;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.stereotype.Component;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

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
