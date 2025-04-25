package com.freenote.app.server.auth;

import com.freenote.app.server.http.HttpUpgradeRequest;
import com.freenote.app.server.http.HttpUpgradeResponse;

public interface AcceptHandshakeHandler {
    HttpUpgradeResponse handle(HttpUpgradeRequest request);
}
