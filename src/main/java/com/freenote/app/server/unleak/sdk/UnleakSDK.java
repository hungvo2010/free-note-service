package com.freenote.app.server.unleak.sdk;

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
}
