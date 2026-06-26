package com.freedraw.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.freedraw.models.enums.DraftRequestType;
import com.freenote.app.server.model.app.AppResponseData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@AllArgsConstructor
public class DraftResponseData extends AppResponseData {
    private String draftId;
    private String draftName;
    private DraftRequestType requestType;
    private String senderId;
    private DraftResponseContent data;

    public DraftResponseData(String draftId, String draftName, List<ShapeData> shapes) {
        this.draftId = draftId;
        this.draftName = draftName;
        this.data = new DraftResponseContent(shapes);
    }

    public DraftResponseData(HeartbeatMsg heartbeat) {

    }

    public DraftResponseData() {
    }
}
