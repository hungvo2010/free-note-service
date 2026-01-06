<<<<<<<< HEAD:unrelated/src/main/java/com/unleak/sdk/NumericCondition.java
package com.unleak.sdk;
========
package sdk;
>>>>>>>> 25da83b6c9f9182369f7ea2b7f7c01d4d535bd32:trial/src/main/java/sdk/NumericCondition.java

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
