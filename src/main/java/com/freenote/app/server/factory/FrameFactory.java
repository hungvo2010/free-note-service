package com.freenote.app.server.factory;

import java.awt.*;

public interface FrameFactory {
    Frame createTextFrame(String text);
    Frame createBinaryFrame(byte[] data);
    Frame createPingFrame(byte[] data);
}