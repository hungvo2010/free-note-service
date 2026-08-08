package com.freenote.app.server.frames.base;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public class FrameBuilder {
    boolean fin = true;
    private boolean rsv1 = false;
    private boolean rsv2 = false;
    private boolean rsv3 = false;
    private int opcode = 0;
    private boolean masked = false;
    private long payloadLength = 0;
    byte[] maskingKey = null;
    private byte[] payloadData = null;
}

