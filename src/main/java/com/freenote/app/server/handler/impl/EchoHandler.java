package com.freenote.app.server.handler.impl;


import com.freenote.app.server.frames.ClientFrame;
import com.freenote.app.server.frames.FrameType;
import com.freenote.app.server.frames.TextFrame;
import com.freenote.app.server.frames.base.WebSocketFrame;
import com.freenote.app.server.handler.URIHandler;
import com.freenote.app.server.util.FrameUtil;
import io.NoHeaderObjectOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class EchoHandler implements URIHandler {
    private static final Logger log = LogManager.getLogger(EchoHandler.class);

    @Override
    public void handle(InputStream inputStream, OutputStream outputStream) {
        try {
            var reader = new BufferedReader(new InputStreamReader(inputStream));
            while (reader.ready()) {
                byte[] data = new byte[70000];
                int byteNumber = inputStream.read(data);
                if (byteNumber == -1) {
                    log.warn("End of stream reached");
                    break;
                }

                byte[] actualData = Arrays.copyOfRange(data, 0, byteNumber);
                WebSocketFrame frame = new ClientFrame(actualData);

                log.info("FIN: {}", frame.isFin());
                log.info("Opcode: {} - {}", frame.getOpcode(), FrameType.fromHexValue(frame.getOpcode()));
                log.info("Masked: {}", frame.isMasked());
                log.info("Payload Length: {}", frame.getPayloadLength());
                log.info("Masking Key: {}", Arrays.toString(frame.getMaskingKey()));
                log.info("Raw Payload (masked): {}", Arrays.toString(frame.getPayloadData()));

                byte[] payload = frame.isMasked() ? FrameUtil.maskPayload(frame.getPayloadData(), frame.getMaskingKey()) : frame.getPayloadData();
                log.info("Unmasked Payload: {}", Arrays.toString(payload));
                log.info("Payload as Text: {}", new String(payload, StandardCharsets.UTF_8));

                var objectOutputStream = new NoHeaderObjectOutputStream(outputStream);
                log.info("Writing to output stream" + " with payload: {}", new String(payload, StandardCharsets.UTF_8));
                objectOutputStream.writeObject(TextFrame.createServerFrame(payload));
                objectOutputStream.flush();
            }
        } catch (IOException e) {
            log.error("Error handling input stream", e);
        }
    }
}
