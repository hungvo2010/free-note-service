<<<<<<<< HEAD:unrelated/src/main/java/com/unleak/sdk/ContainsCondition.java
package com.unleak.sdk;
========
package sdk;
>>>>>>>> 25da83b6c9f9182369f7ea2b7f7c01d4d535bd32:trial/src/main/java/sdk/ContainsCondition.java

import java.util.List;

public class ContainsCondition implements SingleCondition {
    private final boolean isInvert;
    private final String expectKey;
    private final List<Object> expectedValues;

    public ContainsCondition(boolean isInvert, String expectKey, List<Object> expectedValues) {
        this.isInvert = isInvert;
        this.expectedValues = expectedValues;
        this.expectKey = expectKey;
    }

    @Override
    public boolean evaluate(CompositeInput input) {
        var inputValue = input.getValue(this.expectKey);
        return isInvert != expectedValues.contains(inputValue);
    }
}
