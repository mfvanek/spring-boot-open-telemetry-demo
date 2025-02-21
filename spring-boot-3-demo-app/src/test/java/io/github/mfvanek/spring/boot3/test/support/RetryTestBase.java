/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot3.test.support;

import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test-retry")
public abstract class RetryTestBase extends AbstractTestBase {

}
