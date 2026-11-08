/*
 * Copyright (c) 2020-2026. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot.traceid.servlet;

import io.github.mfvanek.spring.boot.traceid.TraceIdResponseHeaders;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TraceIdInResponseServletFilterTest {

    private static final String TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";

    @Test
    void shouldAddTraceIdHeaderWhenCurrentSpanExists() throws IOException, ServletException {
        final TraceIdInResponseServletFilter filter = new TraceIdInResponseServletFilter(mockTracerWithCurrentSpan());
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getHeader(TraceIdResponseHeaders.TRACE_ID_HEADER_NAME))
            .isEqualTo(TRACE_ID);
        assertThat(chain.getRequest())
            .isSameAs(request);
    }

    @Test
    void shouldNotAddHeaderWithoutCurrentSpan() throws IOException, ServletException {
        final TraceIdInResponseServletFilter filter = new TraceIdInResponseServletFilter(mock(Tracer.class));
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getHeaderNames())
            .doesNotContain(TraceIdResponseHeaders.TRACE_ID_HEADER_NAME);
        assertThat(chain.getRequest())
            .isSameAs(request);
    }

    @Test
    void shouldNotAddHeaderForNoopSpan() throws IOException, ServletException {
        final TraceIdInResponseServletFilter filter = new TraceIdInResponseServletFilter(Tracer.NOOP);
        final MockHttpServletRequest request = new MockHttpServletRequest();
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getHeaderNames())
            .doesNotContain(TraceIdResponseHeaders.TRACE_ID_HEADER_NAME);
        assertThat(chain.getRequest())
            .isSameAs(request);
    }

    @Test
    void shouldContinueChainForNonHttpResponse() throws IOException, ServletException {
        final TraceIdInResponseServletFilter filter = new TraceIdInResponseServletFilter(mockTracerWithCurrentSpan());
        final ServletRequest request = mock(ServletRequest.class);
        final ServletResponse response = mock(ServletResponse.class);
        final MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(chain.getResponse())
            .isSameAs(response);
    }

    private static Tracer mockTracerWithCurrentSpan() {
        final TraceContext traceContext = mock(TraceContext.class);
        when(traceContext.traceId()).thenReturn(TRACE_ID);
        final Span span = mock(Span.class);
        when(span.context()).thenReturn(traceContext);
        final Tracer tracer = mock(Tracer.class);
        when(tracer.currentSpan()).thenReturn(span);
        return tracer;
    }
}
