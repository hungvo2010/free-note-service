package com.freedraw.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.freedraw.models.enums.DraftRequestType;
import com.freenote.app.server.model.TraceResponseData;
import lombok.*;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public class DraftResponseData extends TraceResponseData {
    private String draftId;
    private String draftName;
    private DraftRequestType requestType;
    private DraftResponseContent data;

    public DraftResponseData(String draftId, String draftName, List<ShapeData> shapes) {
        this.draftId = draftId;
        this.draftName = draftName;
        this.data = new DraftResponseContent(shapes);
    }
}
