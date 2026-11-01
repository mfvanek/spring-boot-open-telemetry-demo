/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot3.test.service;

import java.io.Serial;

class RetriesExhaustedException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    RetriesExhaustedException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
