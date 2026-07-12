package com.hust.commonlibrary.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;


public class DateUtil extends JsonDeserializer<Instant> {
    private static final DateTimeFormatter ISO_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String date = p.getText();
        LocalDate localDate = LocalDate.parse(date, ISO_DATE_FORMAT);
        return localDate.atStartOfDay().toInstant(ZoneOffset.UTC);
    }

}