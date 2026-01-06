<<<<<<<< HEAD:unrelated/src/main/java/com/unleak/sdk/CompositeInput.java
package com.unleak.sdk;
========
package sdk;
>>>>>>>> 25da83b6c9f9182369f7ea2b7f7c01d4d535bd32:trial/src/main/java/sdk/CompositeInput.java

import java.util.HashMap;
import java.util.Map;

public class CompositeInput {
    private Map<String, Object> inputMap = new HashMap<>();

    public Object getValue(String interestedKey) {
        return this.inputMap.get(interestedKey);
    }

    public void addValue(String key, Object value) {
        this.inputMap.put(key, value);
    }
}
