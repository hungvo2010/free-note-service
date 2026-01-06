<<<<<<<< HEAD:unrelated/src/main/java/com/unleak/sdk/Operator.java
package com.unleak.sdk;
========
package sdk;
>>>>>>>> 25da83b6c9f9182369f7ea2b7f7c01d4d535bd32:trial/src/main/java/sdk/Operator.java

public enum Operator {
    NUM_EQ {
        @Override
        public boolean evaluate(int a, int b) {
            return a == b;
        }
    },
    NUM_GT {
        @Override
        public boolean evaluate(int a, int b) {
            return a > b;
        }
    },
    NUM_GTE {
        @Override
        public boolean evaluate(int a, int b) {
            return a >= b;
        }
    },
    NUM_LT {
        @Override
        public boolean evaluate(int a, int b) {
            return a < b;
        }
    },
    NUM_LTE {
        @Override
        public boolean evaluate(int a, int b) {
            return a <= b;
        }
    };

    public abstract boolean evaluate(int a, int b);
}
