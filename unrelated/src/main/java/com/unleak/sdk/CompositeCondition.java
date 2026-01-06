<<<<<<<< HEAD:unrelated/src/main/java/com/unleak/sdk/CompositeCondition.java
package com.unleak.sdk;
========
package sdk;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
>>>>>>>> 25da83b6c9f9182369f7ea2b7f7c01d4d535bd32:trial/src/main/java/sdk/CompositeCondition.java

import java.util.List;

public class CompositeCondition {
    private static final Logger log = LogManager.getLogger(CompositeCondition.class);
    private final List<SingleCondition> conditions = List.of(
            new ContainsCondition(false, "productId", List.of("prod_123", "prod_456")),
            new NumericCondition(false, "appVersion", 10, Operator.NUM_EQ),
            new DateCondition(false, "releaseDate", "2023-01-01", DateOperator.DATE_AFTER),
            new StringCondition(false, "deviceModel", "Pixel 61", StringOperator.STR_CONTAINS),
            new SemverCondition()
    );

    public boolean evaluate(CompositeInput input) {
        for (SingleCondition condition : conditions) {
            if (!condition.evaluate(input)) {
                log.info("Condition failed: {}", condition);
                return false;
            }
        }
        return true;
    }

    // There are 15 types of operators available: basic (IN, NOT_IN), numeric (NUM_EQ, NUM_GT, NUM_GTE, NUM_LT, NUM_LTE), date/time (DATE_AFTER, DATE_BEFORE), string (STR_CONTAINS, STR_ENDS_WITH, STR_STARTS_WITH), and semantic versioning (SEMVER_EQ, SEMVER_GT, SEMVER_LT). All operators can be negated with a NOT prefix for inverted conditions.

    // multiple strategies to combine conditions: use OR logic
    //
}
