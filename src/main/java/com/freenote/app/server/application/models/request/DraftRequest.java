package com.freenote.app.server.application.models.request;

import com.freenote.app.server.application.models.enums.RequestType;
import lombok.Data;
import lombok.Getter;

import java.net.Socket;

@Getter
public class DraftRequest {
    private String draftId;
    private RequestType requestType;
    private DraftContent content;

    public DraftRequestMedata getMetaData() {
        return null;
    }

    @Data
    public static class DraftRequestMedata {
        private String ipAddress;
        private String port;
        private Socket socket;
    }
}
