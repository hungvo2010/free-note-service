package com.freenote.app.server.handler.impl;


import com.freenote.annotations.URIHandlerImplementation;
import com.freenote.app.server.factory.ClientFrameFactory;
import com.freenote.app.server.factory.ServerFrameFactory;
import com.freenote.app.server.frames.FrameType;
import com.freenote.app.server.frames.base.WebSocketFrame;
import com.freenote.app.server.handler.URIHandler;
import com.freenote.app.server.util.FrameUtil;
import com.freenote.app.server.util.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@URIHandlerImplementation("/example")
public class EchoHandler implements URIHandler {
    private static final Logger log = LogManager.getLogger(EchoHandler.class);
    private static final ServerFrameFactory serverFrameFactory = new ServerFrameFactory();
    private static final ClientFrameFactory clientFrameFactory = new ClientFrameFactory();

    @Override
    public boolean handle(InputStream inputStream, OutputStream outputStream) {
        try {
            var reader = new BufferedReader(new InputStreamReader(inputStream));
            while (reader.ready()) {
                byte[] actualData = getRawBytes(inputStream);
                if (actualData == null) return false;
                WebSocketFrame frame = clientFrameFactory.createFrameFromBytes(actualData);

                log.info("FIN: {}", frame.isFin());
                log.info("Opcode: {} - {}", frame.getOpcode(), FrameType.fromHexValue(frame.getOpcode()));
                log.info("Masked: {}", frame.isMasked());
                log.info("Payload Length: {}", frame.getPayloadLength());
                log.info("Masking Key: {}", Arrays.toString(frame.getMaskingKey()));
                log.info("Raw Payload (masked): {}", Arrays.toString(frame.getPayloadData()));

                byte[] payload = frame.isMasked() ? FrameUtil.maskPayload(frame.getPayloadData(), frame.getMaskingKey()) : frame.getPayloadData();
                log.info("Unmasked Payload: {}", Arrays.toString(payload));
                log.info("Payload as Text: {}", new String(payload, StandardCharsets.UTF_8));

                log.info("Writing to output stream" + " with payload: {}", new String(payload, StandardCharsets.UTF_8));
                log.info("===========================================================================");
                IOUtils.writeOutPut(outputStream, serverFrameFactory.createTextFrame(new String(payload, StandardCharsets.UTF_8)));
            }
//            log.info("Input stream handling completed successfully.");
            return true;
        } catch (IOException e) {
            log.error("Error handling input stream", e);
            return false;
        }
    }

    @Override
    public boolean continuationHandler(List<WebSocketFrame> clientFrame, InputStream inputStream, OutputStream outputStream) {
        return false;
    }

    public byte[] getRawBytes(InputStream inputStream) throws IOException {
        byte[] data = new byte[70000];
        int byteNumber = inputStream.read(data);
        if (byteNumber <= 0) {
            log.warn("End of stream reached");
            return null;
        }
        return Arrays.copyOfRange(data, 0, byteNumber);
    }
}
