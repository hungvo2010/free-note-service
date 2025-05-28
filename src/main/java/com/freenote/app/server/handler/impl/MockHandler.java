package com.freenote.app.server.handler.impl;

import com.freenote.app.server.handler.URIHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;

public class MockHandler implements URIHandler {
    private static final Logger log = LogManager.getLogger(MockHandler.class);

    @Override
    public void handle(InputStream inputStream, OutputStream outputStream) {
        try {
            var reader = new BufferedReader(new InputStreamReader(inputStream));
            log.info(outputStream.getClass().getCanonicalName());
            log.info(inputStream.getClass().getCanonicalName());
            String data;
            while (reader.ready()) {
                data = reader.readLine();
                log.info("Reading from input stream: {}", data);
                outputStream.write((data + System.lineSeparator()).getBytes());
                outputStream.flush();
            }
        } catch (IOException e) {
            log.error("Error handling input stream", e);
        }
    }
}
