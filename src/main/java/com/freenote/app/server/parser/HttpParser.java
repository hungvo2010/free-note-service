package com.freenote.app.server.parser;

import com.freenote.app.server.http.HttpUpgradeRequest;

import java.io.IOException;
import java.io.InputStream;

public interface HttpParser {
     HttpUpgradeRequest parse(InputStream inputStream) throws IOException;
}
