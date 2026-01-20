package com.freedraw.validation;

import com.freedraw.dto.DraftRequestContent;
import com.freedraw.dto.DraftRequestData;
import com.freedraw.dto.DraftResponseData;
import com.freedraw.dto.ShapeData;
import com.freenote.app.server.util.JSONUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Schema Validation Tests")
class SchemaValidationTest {
    
    private static RegistrySchemaValidator validator;
    
    @BeforeAll
    static void setup() {
        validator = new RegistrySchemaValidator();
    }
    
    @Test
    @DisplayName("Valid CONNECT request should pass validation")
    void testValidConnectRequest() {
        DraftRequestData request = new DraftRequestData();
        request.setDraftRequestType(1); // CONNECT
        request.setDraftId("draft-123");
        request.setDraftName("My Drawing");
        request.setContent(new DraftRequestContent());
        
        String json = JSONUtils.toJSONString(request);
        System.out.println("CONNECT Request JSON: " + json);
        var result = validator.validateRequest(json);
        
        assertTrue(result.isValid(), "CONNECT request should be valid: " + result.getErrorMessage());
    }
    
    @Test
    @DisplayName("Valid ADD request with shapes should pass validation")
    void testValidAddRequestWithShapes() {
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
        System.out.println("ADD Request JSON: " + json);
        var result = validator.validateRequest(json);
        
        assertTrue(result.isValid(), "ADD request with shapes should be valid: " + result.getErrorMessage());
    }
    
    @Test
    @DisplayName("Valid UPDATE request should pass validation")
    void testValidUpdateRequest() {
        DraftRequestData request = new DraftRequestData();
        request.setDraftRequestType(3); // UPDATE
        request.setDraftId("draft-123");
        
        List<ShapeData> shapes = new ArrayList<>();
        ShapeData shape = new ShapeData();
        shape.setShapeId("shape-001");
        shape.setType("circle");
        
        HashMap<String, Object> content = new HashMap<>();
        content.put("x", 400);
        content.put("y", 300);
        content.put("radius", 50);
        shape.setContent(content);
        
        shapes.add(shape);
        
        DraftRequestContent requestContent = new DraftRequestContent();
        requestContent.setShapes(shapes);
        request.setContent(requestContent);
        
        String json = JSONUtils.toJSONString(request);
        System.out.println("UPDATE Request JSON: " + json);
        var result = validator.validateRequest(json);
        
        assertTrue(result.isValid(), "UPDATE request should be valid: " + result.getErrorMessage());
    }
    
    @Test
    @DisplayName("Valid REMOVE request should pass validation")
    void testValidRemoveRequest() {
        DraftRequestData request = new DraftRequestData();
        request.setDraftRequestType(4); // REMOVE
        request.setDraftId("draft-123");
        
        List<ShapeData> shapes = new ArrayList<>();
        ShapeData shape = new ShapeData();
        shape.setShapeId("shape-001");
        shapes.add(shape);
        
        DraftRequestContent requestContent = new DraftRequestContent();
        requestContent.setShapes(shapes);
        request.setContent(requestContent);
        
        String json = JSONUtils.toJSONString(request);
        System.out.println("REMOVE Request JSON: " + json);
        var result = validator.validateRequest(json);
        
        assertTrue(result.isValid(), "REMOVE request should be valid: " + result.getErrorMessage());
    }
    
    @Test
    @DisplayName("Request without requestType should fail validation")
    void testInvalidRequestMissingType() {
        String json = "{\"draftId\":\"draft-123\",\"draftName\":\"My Drawing\"}";
        System.out.println("Invalid Request JSON: " + json);
        var result = validator.validateRequest(json);
        
        assertFalse(result.isValid(), "Request without requestType should be invalid");
        assertNotNull(result.getErrorMessage());
        System.out.println("Expected validation error: " + result.getErrorMessage());
    }
    
    @Test
    @DisplayName("Valid response should pass validation")
    void testValidResponse() {
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
        
        DraftResponseData response = new DraftResponseData("draft-123", "My Drawing", shapes);
        
        String json = JSONUtils.toJSONString(response);
        System.out.println("Response JSON: " + json);
        var result = validator.validateResponse(json);
        
        assertTrue(result.isValid(), "Response should be valid: " + result.getErrorMessage());
    }
    
    @Test
    @DisplayName("Multiple shapes in request should pass validation")
    void testMultipleShapes() {
        DraftRequestData request = new DraftRequestData();
        request.setDraftRequestType(2); // ADD
        request.setDraftId("draft-123");
        
        List<ShapeData> shapes = new ArrayList<>();
        
        // Rectangle
        ShapeData rect = new ShapeData();
        rect.setShapeId("shape-001");
        rect.setType("rectangle");
        HashMap<String, Object> rectContent = new HashMap<>();
        rectContent.put("x", 100);
        rectContent.put("y", 150);
        rectContent.put("width", 200);
        rectContent.put("height", 100);
        rect.setContent(rectContent);
        shapes.add(rect);
        
        // Circle
        ShapeData circle = new ShapeData();
        circle.setShapeId("shape-002");
        circle.setType("circle");
        HashMap<String, Object> circleContent = new HashMap<>();
        circleContent.put("x", 400);
        circleContent.put("y", 300);
        circleContent.put("radius", 50);
        circle.setContent(circleContent);
        shapes.add(circle);
        
        DraftRequestContent requestContent = new DraftRequestContent();
        requestContent.setShapes(shapes);
        request.setContent(requestContent);
        
        String json = JSONUtils.toJSONString(request);
        System.out.println("Multiple Shapes Request JSON: " + json);
        var result = validator.validateRequest(json);
        
        assertTrue(result.isValid(), "Request with multiple shapes should be valid: " + result.getErrorMessage());
    }
}
