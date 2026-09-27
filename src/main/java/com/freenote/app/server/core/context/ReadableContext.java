package com.freenote.app.server.core.context;

import com.freenote.app.server.model.http.HttpUpgradeRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Builder
public class ReadableContext {
    @Setter
    private HttpUpgradeRequest httpUpgradeRequest;

    public boolean isHandshakeComplete() {
        return httpUpgradeRequest != null;
    }
}
