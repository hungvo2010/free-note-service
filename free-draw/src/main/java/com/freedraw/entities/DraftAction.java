package com.freedraw.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.freedraw.dto.DraftRequestContent;
import com.freedraw.dto.ShapeData;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.*;

@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public class DraftAction {
    private static final String ALL_SHAPES_KEY = "allShapes";
    
    private List<ShapeData> shapes = new ArrayList<>();
    private final Map<String, Object> actionData = new HashMap<>();

    public DraftAction(DraftRequestContent requestContent) {
        this.shapes = new ArrayList<>(requestContent.getShapes());
    }
    
    public DraftAction(List<ShapeData> allShapes) {
        this.shapes = new ArrayList<>(allShapes);
        this.actionData.put(ALL_SHAPES_KEY, true);
    }

    public void putData(String key, Object value) {
        actionData.put(key, value);
    }

    public List<ShapeData> getShapes() {
        return Collections.unmodifiableList(shapes);
    }
}
