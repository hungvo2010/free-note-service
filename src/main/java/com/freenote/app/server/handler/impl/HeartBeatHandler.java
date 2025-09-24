package com.freenote.app.server.handler.impl;


import com.freenote.annotations.URIHandlerImplementation;
import com.freenote.app.server.factory.ServerFrameFactory;
import com.freenote.app.server.frames.FrameType;
import com.freenote.app.server.frames.base.DataFrame;
import com.freenote.app.server.frames.base.WebSocketFrame;
import com.freenote.app.server.handler.URIHandler;
import com.freenote.app.server.util.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import static com.freenote.app.server.util.IOUtils.getRawBytes;

@URIHandlerImplementation("/heartbeat")
public class HeartBeatHandler implements URIHandler {
    private final Logger log = LogManager.getLogger(HeartBeatHandler.class);
    private final ServerFrameFactory serverFactory = new ServerFrameFactory();

    @Override
    public boolean handle(InputStream inputStream, OutputStream outputStream) {
        try {
            if (inputStream.available() == 0) {
                return true; // No data, don't block
            }

            var rawBytes = getRawBytes(inputStream);
            var frame = DataFrame.fromRawFrameBytes(rawBytes);
            log.info("Received frame with opcode: {}", FrameType.fromOpCode(frame.getOpcode()));
            if (frame.getOpcode() == FrameType.PING.getOpCode()) {
                IOUtils.writeOutPut(outputStream, serverFactory.createPongFrame());
            }
            IOUtils.writeOutPut(outputStream, serverFactory.createTextFrame("Heartbeat acknowledged"));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean continuationHandler(List<WebSocketFrame> clientFrame, InputStream inputStream, OutputStream outputStream) {
        return false;
    }
}
