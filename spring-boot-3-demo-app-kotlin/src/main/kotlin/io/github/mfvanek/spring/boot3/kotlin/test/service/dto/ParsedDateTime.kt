package io.github.mfvanek.spring.boot3.kotlin.test.service.dto

import java.time.LocalDateTime

data class ParsedDateTime(
    val year: Int,
    val monthValue: Int,
    val dayOfMonth: Int,
    val hour: Int,
    val minute: Int
)
fun ParsedDateTime.toLocalDateTime(): LocalDateTime = LocalDateTime.of(year, monthValue, dayOfMonth, hour, minute)
