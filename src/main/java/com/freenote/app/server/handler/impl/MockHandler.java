package com.freenote.app.server.handler.impl;

import com.freenote.app.server.handler.URIHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.time.LocalDateTime;

public class MockHandler implements URIHandler {
    private static final Logger log = LogManager.getLogger(MockHandler.class);

    @Override
    public void handle(InputStream inputStream, OutputStream outputStream) {
        try {
            var reader = new BufferedReader(new InputStreamReader(inputStream));
            var output = new PrintWriter(outputStream, true);
//            log.info(outputStream.getClass().getCanonicalName());
            String data;
            while (true) {
                data = reader.readLine();
                log.info("Reading from input stream: " + data);
                outputStream.write((data + System.lineSeparator()).getBytes());
                outputStream.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
