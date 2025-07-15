package com.freenote.app.server.frames.base;

import com.freenote.app.server.util.FrameUtil;

import java.io.IOException;
import java.io.ObjectOutput;

import static com.freenote.app.server.util.FrameUtil.getFramePayloadLengthSupplier;

public class DataFrame extends WebSocketFrame {

    @Override
    protected void parsePayloadLength(byte[] bytes) {
        masked = ((bytes[1] & 0x80) >> 7) == 1;
        payloadLength = FrameUtil.parsePayloadLength(bytes);
    }

    @Override
    public void writeFrameMaskHeader(ObjectOutput out) throws IOException {
        var maskByte = masked ? (byte) 0x80 : (byte) 0x00;
        var secondByte = (byte) (maskByte | (byte) (getFramePayloadLengthSupplier().applyAsLong(payloadLength)));
        out.writeByte(secondByte);
    }


    @Override
    public void writePayloadLength(ObjectOutput out) throws IOException {
        if (payloadLength < 65536) {
            out.writeShort((short) payloadLength);
            return;
        }
        out.writeLong(payloadLength);
    }

    @Override
    public void writePayload(ObjectOutput out) throws IOException {
            out.write(masked ? FrameUtil.maskPayload(payloadData, maskingKey) : payloadData);
    }
}
