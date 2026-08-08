package com.freedraw.validation;

import com.freedraw.dto.DraftRequestContent;
import com.freedraw.dto.DraftRequestData;
import com.freedraw.dto.DraftResponseData;
import com.freedraw.dto.ShapeData;
import com.freenote.app.server.util.JSONUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Schema Structure Tests - No Registry Required")
class SchemaStructureTest {
    
    @Test
    @DisplayName("DraftRequestData should serialize to correct JSON structure")
    void testRequestSerialization() {
        DraftRequestData request = new DraftRequestData();
        request.setDraftRequestType(2); // ADD
        request.setDraftId("draft-123");
        request.setDraftName("My Drawing");
        
        List<ShapeData> shapes = new ArrayList<>();
        ShapeData shape = new ShapeData();
        shape.setShapeId("shape-001");
        shape.setType("rectangle");
        
        HashMap<String, Object> content = new HashMap<>();
        content.put("x", 100);
        content.put("y", 150);
        content.put("width", 200);
        content.put("height", 100);
        shape.setContent(content);
        
        shapes.add(shape);
        
        DraftRequestContent requestContent = new DraftRequestContent();
        requestContent.setShapes(shapes);
        request.setContent(requestContent);
        
        String json = JSONUtils.toJSONString(request);
        System.out.println("Request JSON: " + json);
        
        // Verify JSON contains expected fields
        assertTrue(json.contains("\"draftId\":\"draft-123\""));
        assertTrue(json.contains("\"draftName\":\"My Drawing\""));
        assertTrue(json.contains("\"requestType\":2"));
        assertTrue(json.contains("\"shapeId\":\"shape-001\""));
        assertTrue(json.contains("\"type\":\"rectangle\""));
    }
    
    @Test
    @DisplayName("DraftResponseData should serialize to correct JSON structure")
    void testResponseSerialization() {
        List<ShapeData> shapes = new ArrayList<>();
        ShapeData shape = new ShapeData();
        shape.setShapeId("shape-001");
        shape.setType("rectangle");
        
        HashMap<String, Object> content = new HashMap<>();
        content.put("x", 100);
        content.put("y", 150);
        shape.setContent(content);
        
        shapes.add(shape);
        
        DraftResponseData response = new DraftResponseData("draft-123", "My Drawing", shapes);
        
        String json = JSONUtils.toJSONString(response);
        System.out.println("Response JSON: " + json);
        
        // Verify JSON contains expected fields
        assertTrue(json.contains("\"draftId\":\"draft-123\""));
        assertTrue(json.contains("\"draftName\":\"My Drawing\""));
        assertTrue(json.contains("\"shapeId\":\"shape-001\""));
    }
    
    @Test
    @DisplayName("ShapeData content should be flexible")
    void testShapeDataFlexibility() {
        ShapeData shape = new ShapeData();
        shape.setShapeId("shape-001");
        shape.setType("custom");
        
        HashMap<String, Object> content = new HashMap<>();
        content.put("customField1", "value1");
        content.put("customField2", 123);
        content.put("nested", Map.of("key", "value"));
        shape.setContent(content);
        
        String json = JSONUtils.toJSONString(shape);
        System.out.println("Shape JSON: " + json);
        
        assertTrue(json.contains("\"customField1\":\"value1\""));
        assertTrue(json.contains("\"customField2\":123"));
    }
}
