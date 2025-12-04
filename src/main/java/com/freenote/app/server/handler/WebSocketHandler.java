package com.freenote.app.server.handler;

public interface WebSocketHandler {
    void onClose();

    void onPing(byte[] payload);

    void onPong(byte[] payload);

    void onContinue(byte[] data);

    void onText(String message);

    void onBinary(byte[] data);
}
