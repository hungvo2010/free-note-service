package com.freenote.app.server.handler.impl;


import com.freenote.annotations.URIHandlerImplementation;
import com.freenote.app.server.frames.factory.ClientFrameFactory;
import com.freenote.app.server.frames.factory.ServerFrameFactory;
import com.freenote.app.server.frames.FrameType;
import com.freenote.app.server.frames.base.WebSocketFrame;
import com.freenote.app.server.handler.URIHandler;
import com.freenote.app.server.util.FrameUtil;
import com.freenote.app.server.util.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static com.freenote.app.server.util.IOUtils.getRawBytes;

@URIHandlerImplementation("/echo")
public class EchoHandlerImpl implements URIHandler {
    private static final Logger log = LogManager.getLogger(EchoHandlerImpl.class);
    private static final ServerFrameFactory serverFrameFactory = new ServerFrameFactory();
    private static final ClientFrameFactory clientFrameFactory = new ClientFrameFactory();

    @Override
    public boolean handle(InputStream inputStream, OutputStream outputStream) {
        try {
            if (inputStream.available() == 0) {
                return false;
            }
            byte[] actualData = getRawBytes(inputStream);
            if (actualData == null) return false;

            log.info("Raw bytes length: {}", actualData.length);
            log.info("First 10 bytes: {}", Arrays.toString(Arrays.copyOf(actualData, Math.min(10, actualData.length))));

            // Show as unsigned values
            for (int i = 0; i < Math.min(10, actualData.length); i++) {
                log.info("Byte[{}]: signed={}, unsigned={}, hex=0x{}",
                        i, actualData[i], actualData[i] & 0xFF, Integer.toHexString(actualData[i] & 0xFF));
            }
            WebSocketFrame frame = clientFrameFactory.createFrameFromBytes(actualData);

            log.info("FIN: {}", frame.isFin());
            log.info("Opcode: {} - {}", frame.getOpcode(), FrameType.fromHexValue(frame.getOpcode()));
            log.info("Masked: {}", frame.isMasked());
            log.info("Payload Length: {}", frame.getPayloadLength());
            log.info("Masking Key: {}", frame.getMaskingKey());

            byte[] payload = frame.isMasked() ? FrameUtil.maskPayload(frame.getPayloadData(), frame.getMaskingKey()) : frame.getPayloadData();
            String content = new String(payload, StandardCharsets.UTF_8);

            log.info("Writing to output stream with payload: {}", content);
            log.info("===========================================================================");
            IOUtils.writeOutPut(outputStream, serverFrameFactory.createTextFrame(content));
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
}
