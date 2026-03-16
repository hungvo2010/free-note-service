package com.freenote.app.server.core.v2;

import com.freenote.app.server.model.http.HttpUpgradeRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.nio.ByteBuffer;

@AllArgsConstructor
@Getter
@NoArgsConstructor
@Setter
public class HandShakeState {
    private boolean isFirstRead = true;
    private boolean isHandshakeComplete = false;
    private HttpUpgradeRequest upgradeRequest;
    private ByteBuffer byteBuffer = ByteBuffer.allocateDirect(2048);
}
