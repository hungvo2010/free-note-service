package com.freedraw.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.freedraw.models.enums.DraftRequestType;
import com.freenote.app.server.model.TraceRequestData;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
public class DraftRequestData extends TraceRequestData {
    private String draftId;
    private String draftName;
    private DraftRequestType draftRequestType;
    private DraftContent content = new DraftContent();


    @JsonSetter("requestType")
    public void setDraftRequestType(int value) {
        this.draftRequestType = DraftRequestType.fromCode(value);
    }
}
