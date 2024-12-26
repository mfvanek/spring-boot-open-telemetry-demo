package io.github.mfvanek.spring.boot2.test.service.dto;

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

    public final int year;
    public final int monthValue;
    public final int dayOfMonth;
    public final int hour;
    public final int minute;
}
