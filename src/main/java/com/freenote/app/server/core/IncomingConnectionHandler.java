package com.freenote.app.server.core;


import com.freenote.app.server.model.LegacyIOWrapper;

import java.io.IOException;

public interface IncomingConnectionHandler {
    void handle(LegacyIOWrapper legacyIOWrapper) throws IOException;
}

