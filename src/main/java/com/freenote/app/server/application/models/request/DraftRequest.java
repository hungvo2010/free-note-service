package com.freenote.app.server.application.models.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.freenote.app.server.application.models.enums.DraftRequestType;
import lombok.Data;
import lombok.Getter;

import java.net.Socket;

@Getter
public class DraftRequest {
    private String draftId;
    private String draftName;
    private DraftRequestType draftRequestType;
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

    @JsonSetter("requestType")
    public void setDraftRequestType(int value) {
        this.draftRequestType = DraftRequestType.fromCode(value);
    }
}
