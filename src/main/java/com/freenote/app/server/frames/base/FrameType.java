package com.freenote.app.server.frames.base;

import com.freenote.app.server.frames.FrameFactory;

public enum FrameType {
    CONTINUATION((byte) 0x0) {
        @Override
        public WebSocketFrame parseFrame(byte[] frame) {
            return parseDataFrame(frame, this);
        }

        @Override
        public void handleVariableLength(FrameBuilder frameBuilder) {
            parseVariableLength(frameBuilder);
        }
    },
    TEXT((byte) 0x1) {
        @Override
        public WebSocketFrame parseFrame(byte[] frame) {
            return parseDataFrame(frame, this);
        }

        @Override
        public void handleVariableLength(FrameBuilder frameBuilder) {

        }
    },
    BINARY((byte) 0x2) {
        @Override
        public WebSocketFrame parseFrame(byte[] frame) {
            return parseDataFrame(frame, this);
        }

        @Override
        public void handleVariableLength(FrameBuilder frameBuilder) {

        }
    },
    CLOSE((byte) 0x8) {
        @Override
        public WebSocketFrame parseFrame(byte[] frame) {
            return parseControlFrame(frame, this);
        }

        @Override
        public void handleVariableLength(FrameBuilder frameBuilder) {

        }
    },
    PING((byte) 0x9) {
        @Override
        public WebSocketFrame parseFrame(byte[] frame) {
            return parseControlFrame(frame, this);
        }

        @Override
        public void handleVariableLength(FrameBuilder frameBuilder) {

        }
    },
    PONG((byte) 0xA) {
        @Override
        public WebSocketFrame parseFrame(byte[] frame) {
            return parseControlFrame(frame, this);
        }

        @Override
        public void handleVariableLength(FrameBuilder frameBuilder) {

        }
    };

    private static void parseVariableLength(FrameBuilder frameBuilder) {
        var payloadLength = frameBuilder.getPayloadData().length;
        var maskByte = frameBuilder.isMasked() ? (byte) 0x80 : (byte) 0x00;
        var secondByte = (byte) (maskByte | (byte) (payloadLength < 126 ? payloadLength : (payloadLength == 126 ? 126 : 127)));
    }

    private final short opcode;

    FrameType(byte opCode) {
        this.opcode = opCode;
    }

    public abstract WebSocketFrame parseFrame(byte[] frame);

    public abstract void handleVariableLength(FrameBuilder frameBuilder);

    private static WebSocketFrame parseDataFrame(byte[] frame, FrameType frameType) {
        return FrameFactory.createDataFrame(frame, frameType);
    }

    private static WebSocketFrame parseControlFrame(byte[] frame, FrameType frameType) {
        return FrameFactory.createControlFrame(frame, frameType);
    }

    public short getOpcode() {
        return opcode;
    }
}
