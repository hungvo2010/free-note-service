package com.freedraw.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.freedraw.entities.DraftAction;
import com.freenote.app.server.model.TraceResponseData;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DraftResponseData extends TraceResponseData {
    private DraftAction action;
}
