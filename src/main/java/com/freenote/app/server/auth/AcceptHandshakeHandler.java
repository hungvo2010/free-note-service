package com.freenote.app.server.auth;

import com.freenote.app.server.model.http.HttpUpgradeRequest;
import com.freenote.app.server.model.http.HttpUpgradeResponse;

public interface AcceptHandshakeHandler {
    HttpUpgradeResponse handle(HttpUpgradeRequest request);
}
