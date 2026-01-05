package com.unleak.sdk;

public class NumericCondition implements SingleCondition {
    private final Operator numericOperator;
    private final boolean isInvert;
    private final int expectNumber;
    private final String expectKey;

    public NumericCondition(boolean isInvert, String expectKey, int expectNumber, Operator numericOperator) {
        this.isInvert = isInvert;
        this.expectKey = expectKey;
        this.expectNumber = expectNumber;
        this.numericOperator = numericOperator;
    }

    @Override
    public boolean evaluate(CompositeInput input) {
        return this.numericOperator.evaluate((Integer) input.getValue(this.expectKey), this.expectNumber) != this.isInvert;
    }
}
