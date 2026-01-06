package com.freedraw.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.freedraw.models.enums.DraftRequestType;
import com.freenote.app.server.core.data.RequestData;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
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
