package com.freenote.app.server;

import com.freenote.app.server.http.HttpUpgradeRequest;
import com.freenote.app.server.http.exceptions.UpgradeParserException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class HttpParser {
    public HttpUpgradeRequest parse(InputStream inputStream) {
        try (var in = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            var httpRequestBuilder = HttpUpgradeRequest.builder();
            var requestEndpoint = in.readLine().split(" ");
            httpRequestBuilder.method(requestEndpoint[0]);
            httpRequestBuilder.uri(requestEndpoint[1]);
            httpRequestBuilder.version(requestEndpoint[2]);
            while (in.ready()) {
                String header = in.readLine();

            }
            return httpRequestBuilder.build();
        } catch (IOException e) {
            throw new UpgradeParserException("Error parsing upgrade request", e);
        }
    }
}
