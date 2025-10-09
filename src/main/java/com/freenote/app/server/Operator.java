package com.freenote.app.server;

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
