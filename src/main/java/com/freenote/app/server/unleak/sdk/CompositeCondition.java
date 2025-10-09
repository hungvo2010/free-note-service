package com.freenote.app.server.unleak.sdk;

import java.util.List;

public class CompositeCondition {
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
                System.out.println("Condition failed: " + condition);
                return false;
            }
        }
        return true;
    }

    // There are 15 types of operators available: basic (IN, NOT_IN), numeric (NUM_EQ, NUM_GT, NUM_GTE, NUM_LT, NUM_LTE), date/time (DATE_AFTER, DATE_BEFORE), string (STR_CONTAINS, STR_ENDS_WITH, STR_STARTS_WITH), and semantic versioning (SEMVER_EQ, SEMVER_GT, SEMVER_LT). All operators can be negated with a NOT prefix for inverted conditions.

    // multiple strategies to combine conditions: use OR logic
    //

    public static void main(String[] args) {
        var compositeCondition = new CompositeCondition();
        var input = new CompositeInput();
        input.addValue("productId", "prod_123");
        input.addValue("appVersion", 10);
        input.addValue("releaseDate", "2023-06-01");
        input.addValue("deviceModel", "Pixel 6");
        input.addValue("appSemver", "1.2.3");

        var result = compositeCondition.evaluate(input);
        System.out.println("Evaluation result: " + result);
    }
}
