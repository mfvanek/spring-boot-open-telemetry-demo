/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot3.test.service;

class RetriesExhaustedException extends RuntimeException {

    RetriesExhaustedException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
