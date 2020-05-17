package io.github.mfvanek.spring.test.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
public class TimeController {

    // http://localhost:8080/current-time
    @GetMapping(path = "/current-time")
    public LocalDateTime getNow() {
        return LocalDateTime.now();
    }
}
