/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot3.kotlin.test.service.dto

import java.time.LocalDateTime

data class ParsedDateTime(
    val year: Int,
    val monthValue: Int,
    val dayOfMonth: Int,
    val hour: Int,
    val minute: Int
)

fun ParsedDateTime.toLocalDateTime(): LocalDateTime =
    LocalDateTime.of(year, monthValue, dayOfMonth, hour, minute)
fun LocalDateTime.toParsedDateTime(): ParsedDateTime =
    ParsedDateTime(year = year, monthValue = month.value, dayOfMonth = dayOfMonth, hour = hour, minute = minute)
