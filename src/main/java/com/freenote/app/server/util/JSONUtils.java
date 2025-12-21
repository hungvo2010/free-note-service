package com.freenote.app.server.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.experimental.UtilityClass;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@UtilityClass
public class JSONUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger log = LogManager.getLogger(JSONUtils.class);

    public static String toJSONString(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }

    public static byte[] toJSONBytes(Object obj) {
        try {
            return objectMapper.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            return new byte[0];
        }
    }

    public static <T> T fromJSON(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.error("Error parsing json: {}", json, e);
            return null;
        }
    }

    public static <T> T fromMap(Object mapObj, Class<T> clazz) {
        try {
            return objectMapper.convertValue(mapObj, clazz);
        } catch (Exception e) {
            log.error("Error converting map to class {}: {}", clazz.getName(), e.getMessage(), e);
            return null;
        }
    }

    // Additional utility methods can be added here
    // The error occurs because messagePayload.getBody().toString() converts the Jackson-deserialized Object (likely a LinkedHashMap) to its toString() representation, producing {requestType=2, content={...}} instead of valid JSON {"requestType":2,...}
}
