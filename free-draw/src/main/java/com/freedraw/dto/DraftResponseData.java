package com.freedraw.dto;

import com.freedraw.entities.DraftAction;
import com.freenote.app.server.core.data.ResponseData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class DraftResponseData extends ResponseData {
    private DraftAction action;
}
