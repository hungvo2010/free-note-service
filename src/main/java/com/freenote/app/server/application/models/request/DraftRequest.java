package com.freenote.app.server.application.models.request;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.freenote.app.server.application.models.enums.DraftRequestType;
import com.freenote.app.server.application.models.request.core.RequestData;
import lombok.Getter;

@Getter
public class DraftRequest extends RequestData {
    private String draftId;
    private String draftName;
    private DraftRequestType draftRequestType;
    private DraftContent content;

    @JsonSetter("requestType")
    public void setDraftRequestType(int value) {
        this.draftRequestType = DraftRequestType.fromCode(value);
    }
}
