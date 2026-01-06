<<<<<<<< HEAD:unrelated/src/main/java/com/unleak/sdk/DateCondition.java
package com.unleak.sdk;
========
package sdk;
>>>>>>>> 25da83b6c9f9182369f7ea2b7f7c01d4d535bd32:trial/src/main/java/sdk/DateCondition.java

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DateCondition implements SingleCondition {
    private static final Logger log = LogManager.getLogger(DateCondition.class);
    private final DateOperator dateOperator;
    private final String expectDateTime;
    private final String expectKey;
    private final boolean isInvert;

    public DateCondition(boolean isInvert, String expectKey, String expectDateTime, DateOperator dateOperator) {
        this.isInvert = isInvert;
        this.expectKey = expectKey;
        this.expectDateTime = expectDateTime;
        this.dateOperator = dateOperator;
    }

    @Override
    public boolean evaluate(CompositeInput input) {
        log.debug("Evaluating DateCondition: key='{}', value='{}', operator='{}', isInvert='{}'", expectKey, expectDateTime, dateOperator, isInvert);
        return this.dateOperator.evaluate(this.expectDateTime, input.getValue(this.expectKey).toString()) != this.isInvert;
    }
}
