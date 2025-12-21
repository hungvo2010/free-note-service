package com.freedraw.models.core;

import com.freenote.app.server.core.ResponseData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class DraftResponseData extends ResponseData {
    private DraftAction action;
}
