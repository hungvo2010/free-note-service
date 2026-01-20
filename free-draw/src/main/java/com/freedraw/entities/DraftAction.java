package com.freedraw.entities;

import com.freedraw.dto.DraftRequestContent;
import com.freedraw.dto.ShapeData;
import lombok.NoArgsConstructor;

import java.util.*;

@NoArgsConstructor
public class DraftAction {
    private List<ShapeData> shapes;
    private Map<String, Object> actionData = new HashMap<>();

    public DraftAction(DraftRequestContent requestContent) {
        this.shapes = new ArrayList<>(requestContent.getShapes());
    }

    public void putData(String key, Object value) {
        actionData.put(key, value);
    }

    public List<ShapeData> getShapes() {
        return Collections.unmodifiableList(shapes);
    }
}
