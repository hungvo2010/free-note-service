package com.freedraw.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.freedraw.models.enums.DraftRequestType;
import com.freenote.app.server.model.TraceResponseData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DraftResponseData extends TraceResponseData {
    private String draftId;
    private String draftName;
    private DraftRequestType actionType;
    private List<ShapeData> shapes = new ArrayList<>();
}
