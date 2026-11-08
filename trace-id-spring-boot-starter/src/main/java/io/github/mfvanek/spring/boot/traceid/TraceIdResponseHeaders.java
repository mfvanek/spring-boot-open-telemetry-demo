/*
 * Copyright (c) 2020-2026. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot.traceid;

/**
 * Names of HTTP response headers populated by this starter.
 */
public final class TraceIdResponseHeaders {

    /**
     * Response header that contains the traceId of the current request.
     */
    public static final String TRACE_ID_HEADER_NAME = "X-TraceId";

    private TraceIdResponseHeaders() {
    }
}
