/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot3.reactive.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Immutable
public class ParsedDateTime {

    private final int year;
    private final int monthValue;
    private final int dayOfMonth;
    private final int hour;
    private final int minute;

    @Nonnull
    public static ParsedDateTime from(final LocalDateTime localDateTime) {
        return new ParsedDateTime(
            localDateTime.getYear(),
            localDateTime.getMonthValue(),
            localDateTime.getDayOfMonth(),
            localDateTime.getHour(),
            localDateTime.getMinute()
        );
    }

    @Nonnull
    public LocalDateTime toLocalDateTime() {
        return LocalDateTime.of(year, monthValue, dayOfMonth, hour, minute);
    }
}
