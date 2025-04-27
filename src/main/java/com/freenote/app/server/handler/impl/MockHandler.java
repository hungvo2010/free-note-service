package com.freenote.app.server.handler.impl;

import com.freenote.app.server.handler.URIHandler;

import java.io.*;

public class MockHandler implements URIHandler {
    @Override
    public void handle(InputStream inputStream, OutputStream outputStream) {
        try {
            var reader = new BufferedReader(new InputStreamReader(inputStream));
            var output = new PrintWriter(outputStream, true);
            while (reader.ready()) {
                output.println(reader.readLine());
            }
//            output.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
