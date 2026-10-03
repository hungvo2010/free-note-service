package com.freenote.app.server.core.nio.state;

import com.freenote.app.server.core.event.ConnectionEvent;
import com.freenote.app.server.handshaker.http.HttpUgrade;
import lombok.AllArgsConstructor;
import lombok.Getter;

public interface ConnectionState {

    ConnectionState transition(ConnectionEvent event, HttpUgrade.HttpUpgradeRequest upgradeRequest);

    class HandShakeState implements ConnectionState {

        @Override
        public ConnectionState transition(ConnectionEvent event, HttpUgrade.HttpUpgradeRequest upgradeRequest) {
            return upgradeRequest != null ? new MessageState(upgradeRequest) : this;
        }
    }

    @AllArgsConstructor
    @Getter
    class MessageState implements ConnectionState {
        private final HttpUgrade.HttpUpgradeRequest request;

        @Override
        public ConnectionState transition(ConnectionEvent event, HttpUgrade.HttpUpgradeRequest upgradeRequest) {
            if (event.getNetworkRequestData().isClosed()) {
                return null;
            }
            return this;
        }
    }
}
