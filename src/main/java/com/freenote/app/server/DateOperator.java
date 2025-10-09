package com.freenote.app.server;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public enum DateOperator {
    DATE_BEFORE {
        @Override
        public boolean evaluate(String expectDateTime, String actualDateTime) {
            var actual = LocalDate.parse(actualDateTime, EXPECTED_FORMAT);
            var expect = LocalDate.parse(expectDateTime, EXPECTED_FORMAT);
            return actual.isBefore(expect);
        }
    },
    DATE_AFTER {
        @Override
        public boolean evaluate(String expectDateTime, String actualDateTime) {
            var actual = LocalDate.parse(actualDateTime, EXPECTED_FORMAT);
            var expect = LocalDate.parse(expectDateTime, EXPECTED_FORMAT);
            return actual.isAfter(expect);
        }
    };

    private static final DateTimeFormatter EXPECTED_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public abstract boolean evaluate(String expectDateTime, String actualDateTime);
}
