package com.unleak.sdk;

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
