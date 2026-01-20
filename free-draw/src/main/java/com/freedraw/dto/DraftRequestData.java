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

import java.util.ArrayList;
import java.util.List;

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
    private DraftRequestContent content = new DraftRequestContent();
    private List<ShapeData> shapes = new ArrayList<>();

    @JsonSetter("requestType")
    public void setDraftRequestType(int value) {
        this.draftRequestType = DraftRequestType.fromCode(value);
    }
}
