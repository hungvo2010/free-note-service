<<<<<<<< HEAD:unrelated/src/main/java/com/unleak/sdk/StringOperator.java
package com.unleak.sdk;
========
package sdk;
>>>>>>>> 25da83b6c9f9182369f7ea2b7f7c01d4d535bd32:trial/src/main/java/sdk/StringOperator.java

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
