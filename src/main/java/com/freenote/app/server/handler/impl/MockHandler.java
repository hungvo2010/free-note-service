package com.freenote.app.server.handler.impl;


import com.freenote.app.server.frames.BaseFrame;
import com.freenote.app.server.frames.TextFrame;
import com.freenote.app.server.handler.URIHandler;
import io.NoHeaderObjectOutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static com.freenote.app.server.frames.FrameUtil.maskPayload;

public class MockHandler implements URIHandler {
    private static final Logger log = LogManager.getLogger(MockHandler.class);

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
                BaseFrame frame = new BaseFrame(actualData);

                log.info("FIN: {}", frame.isFin());
                log.info("Opcode: {}", frame.getOpcode());
                log.info("Masked: {}", frame.isMasked());
                log.info("Payload Length: {}", frame.getPayloadLength());
                log.info("Masking Key: {}", Arrays.toString(frame.getMaskingKey()));
                log.info("Raw Payload (masked): {}", Arrays.toString(frame.getPayloadData()));

                byte[] payload = frame.isMasked() ? maskPayload(frame.getPayloadData(), frame.getMaskingKey()) : frame.getPayloadData();
                log.info("Unmasked Payload: {}", Arrays.toString(payload));
                log.info("Payload as Text: {}", new String(payload, StandardCharsets.UTF_8));

                var objectOutputStream = new NoHeaderObjectOutputStream(outputStream);
                log.info("Writing to output stream" + " with payload: {}", new String(payload, StandardCharsets.UTF_8));
                objectOutputStream.writeObject(new TextFrame("from your websocket".getBytes(StandardCharsets.UTF_8)));
                objectOutputStream.flush();
            }
        } catch (IOException e) {
            log.error("Error handling input stream", e);
        }
    }
}
