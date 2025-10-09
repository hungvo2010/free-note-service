package com.freenote.app.server;

public enum StringOperator {
    STR_CONTAINS {
        @Override
        boolean evaluate(String expectedValue, String inputValue) {
            return expectedValue.contains(inputValue);
        }
    },
    STR_ENDS_WITH {
        @Override
        boolean evaluate(String expectedValue, String inputValue) {
            return expectedValue.endsWith(inputValue);
        }
    },
    STR_STARTS_WITH {
        @Override
        boolean evaluate(String expectedValue, String inputValue) {
            return expectedValue.startsWith(inputValue);
        }
    };

    abstract boolean evaluate(String expectedValue, String inputValue);
}
