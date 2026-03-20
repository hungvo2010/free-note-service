package com.freedraw.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class DraftResponseContent {
    private List<ShapeData> shapes = new ArrayList<>();
}
