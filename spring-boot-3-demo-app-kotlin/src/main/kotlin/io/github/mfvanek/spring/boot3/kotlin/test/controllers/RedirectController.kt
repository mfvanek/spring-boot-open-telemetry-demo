/*
 * Copyright (c) 2020-2025. Ivan Vakhrushev and others.
 * https://github.com/mfvanek/spring-boot-open-telemetry-demo
 *
 * Licensed under the Apache License 2.0
 */

package io.github.mfvanek.spring.boot3.kotlin.test.controllers

import java.net.URI
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class RedirectController {
    // http://localhost:8080/redirect
    @GetMapping(path = ["/redirect"])
    fun redirectToGoogle(): ResponseEntity<Any> {
        val google = URI("https://www.google.com")
        val httpHeaders = HttpHeaders()
        httpHeaders.location = google
        return ResponseEntity(httpHeaders, HttpStatus.SEE_OTHER)
    }
}
