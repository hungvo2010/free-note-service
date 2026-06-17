package com.freedraw.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.freedraw.models.enums.DraftRequestType;
import com.freenote.app.server.model.ws.AppRequestData;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DraftRequestData extends AppRequestData {
    private String draftId;
    private String draftName;
    private String senderId;
    private DraftRequestType draftRequestType;
    private DraftRequestContent content = new DraftRequestContent();

    @JsonSetter("requestType")
    public void setDraftRequestType(int value) {
        this.draftRequestType = DraftRequestType.fromCode(value);
    }

    public boolean isNewUpdate() {
        return draftRequestType == DraftRequestType.UPDATE && (draftId == null || draftId.isEmpty());
    }

    public boolean isConnect() {
        return draftRequestType == DraftRequestType.CONNECT;
    }
}
