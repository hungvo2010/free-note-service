package com.freenote.app.server.application.repository.persistence.disk;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.freenote.app.server.application.models.core.DraftAction;

public class JSONUtility {

    public static DraftAction convertToType(String actionData) {
        try {

            return new ObjectMapper().readValue(actionData, DraftAction.class);
        } catch (JsonProcessingException e) {
            return DraftAction.INVALID;
        }
    }
}