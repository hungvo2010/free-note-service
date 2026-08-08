package com.freenote.app.server.parser;

import com.freenote.app.server.model.http.HttpUpgradeRequest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public interface HttpParser {
    HttpUpgradeRequest parse(InputStream inputStream) throws IOException;

    HttpUpgradeRequest parse(ByteBuffer byteBuffer);
}
