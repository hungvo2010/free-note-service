package com.unleak.sdk;

import com.unleak.sdk.CompositeInput;

public class SemverCondition implements SingleCondition {
    @Override
    public boolean evaluate(CompositeInput input) {
        return true;
    }
}
