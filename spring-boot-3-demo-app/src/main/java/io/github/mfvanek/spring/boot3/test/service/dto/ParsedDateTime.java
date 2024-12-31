package io.github.mfvanek.spring.boot3.test.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
@Builder
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class ParsedDateTime {

    private final int year;
    private final int monthValue;
    private final int dayOfMonth;
    private final int hour;
    private final int minute;
}
