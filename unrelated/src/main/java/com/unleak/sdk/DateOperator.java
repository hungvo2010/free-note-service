<<<<<<<< HEAD:unrelated/src/main/java/com/unleak/sdk/DateOperator.java
package com.unleak.sdk;
========
package sdk;
>>>>>>>> 25da83b6c9f9182369f7ea2b7f7c01d4d535bd32:trial/src/main/java/sdk/DateOperator.java

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
