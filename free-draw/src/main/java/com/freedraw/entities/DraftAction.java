package com.freedraw.entities;

import com.freedraw.dto.DraftRequestContent;
import com.freedraw.models.enums.DraftActionType;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@NoArgsConstructor
public class DraftAction {
    @Getter
    private DraftActionType actionType = DraftActionType.INIT;
    private final Map<String, Object> data = new LinkedHashMap<>();

    public DraftAction(DraftActionType actionType) {
        this.actionType = actionType;
    }

    public DraftAction(DraftRequestContent requestContent) {
        this.actionType = DraftActionType.fromCode(Integer.parseInt(requestContent.getAttribute("type").toString()));
        this.data.putAll(requestContent.getAttributes());
    }

    public void addData(String key, Object value) {
        data.put(key, value);
    }

    public Map<String, Object> getData() {
        return Collections.unmodifiableMap(this.data);
    }
}
