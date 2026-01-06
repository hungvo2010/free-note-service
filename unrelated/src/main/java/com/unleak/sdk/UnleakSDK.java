<<<<<<<< HEAD:unrelated/src/main/java/com/unleak/sdk/UnleakSDK.java
package com.unleak.sdk;
========
package sdk;
>>>>>>>> 25da83b6c9f9182369f7ea2b7f7c01d4d535bd32:trial/src/main/java/sdk/UnleakSDK.java

import java.util.List;

public class UnleakSDK {
    private List<CompositeCondition> conditions;

    public boolean evaluate(CompositeInput input) {
        for (CompositeCondition condition : conditions) {
            if (condition.evaluate(input)) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) {
        var compositeCondition = new CompositeCondition();
        var input = new CompositeInput();
        input.addValue("productId", "prod_123");
        input.addValue("appVersion", 10);
        input.addValue("releaseDate", "2023-06-01");
        input.addValue("deviceModel", "Pixel 6");
        input.addValue("appSemver", "1.2.3");

        UnleakSDK sdk = new UnleakSDK();
        sdk.conditions = List.of(compositeCondition);

        boolean result = sdk.evaluate(input);
        System.out.println("Evaluation result: " + result);
    }
}
