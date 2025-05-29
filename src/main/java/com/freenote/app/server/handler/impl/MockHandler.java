package com.freenote.app.server.handler.impl;

import com.freenote.app.server.frames.BaseFrame;
import com.freenote.app.server.handler.URIHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.BitSet;

public class MockHandler implements URIHandler {
    private static final Logger log = LogManager.getLogger(MockHandler.class);

    @Override
    public void handle(InputStream inputStream, OutputStream outputStream) {
        try {
            var reader = new BufferedReader(new InputStreamReader(inputStream));
            while (reader.ready()) {
                byte[] data = new byte[1014];
                inputStream.read(data);
                var bitset = BitSet.valueOf(data);
                for (int i = 0; i < 16; i++) {
                    System.out.println(bitset.get(i));
                }
                log.info("Reading from input stream: {}", data);
                var sampleFrame = new BaseFrame(data);
                ObjectOutput objectOutput = new ObjectOutputStream(outputStream);
                sampleFrame.writeExternal(objectOutput);
                objectOutput.flush();
            }
        } catch (IOException e) {
            log.error("Error handling input stream", e);
        }
    }
}
