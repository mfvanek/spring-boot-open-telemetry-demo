/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot3.reactive.support;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ScheduledLoggingTask {

    @Scheduled(fixedDelayString = "${app.scheduled-logger.delay-ms:60000}")
    public void logHeartbeat() {
        log.info("Scheduled heartbeat from reactive app");
    }
}


