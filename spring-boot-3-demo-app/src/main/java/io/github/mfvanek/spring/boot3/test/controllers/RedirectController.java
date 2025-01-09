package io.github.mfvanek.spring.boot3.test.controllers;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URISyntaxException;

@RestController
public class RedirectController {

    // http://localhost:8080/redirect
    @GetMapping(path = "/redirect")
    public ResponseEntity<Object> redirectToGoogle() throws URISyntaxException {
        final URI google = new URI("https://www.google.com");
        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setLocation(google);
        return new ResponseEntity<>(httpHeaders, HttpStatus.SEE_OTHER);
    }
}
