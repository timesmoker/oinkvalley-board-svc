package com.oinkvalley.board_svc.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Validates stored ProseMirror JSON against {@code content-schema.json}.
 * Schema SOT: {@code board/content-schema.json}.
 */
@Component
public class ProseMirrorContentValidator {

    private static final String SCHEMA_RESOURCE = "content-schema.json";

    private final BoardContentSchema schema;

    public ProseMirrorContentValidator(ObjectMapper objectMapper) {
        this.schema = loadSchema(objectMapper);
    }

    public void validate(Map<String, Object> content) {
        if (content == null || content.isEmpty()) {
            throw invalid("content is required");
        }
        if (!schema.rootType().equals(content.get("type"))) {
            throw invalid("root type must be " + schema.rootType());
        }
        NodeCounter counter = new NodeCounter();
        validateNode(content, 0, counter);
    }

    @SuppressWarnings("unchecked")
    private void validateNode(Map<String, Object> node, int depth, NodeCounter counter) {
        if (depth > schema.maxDepth()) {
            throw invalid("content exceeds max depth");
        }
        if (counter.count++ > schema.maxNodes()) {
            throw invalid("content exceeds max node count");
        }

        Object typeObj = node.get("type");
        if (!(typeObj instanceof String type) || type.isBlank()) {
            throw invalid("node type is required");
        }

        String kind = schema.nodeKind(type);
        if (kind == null) {
            throw invalid("disallowed node type: " + type);
        }

        switch (kind) {
            case "text" -> validateTextNode(node);
            case "container" -> validateChildContent(node, depth, counter);
            case "leaf" -> { /* no children */ }
            default -> throw invalid("unsupported node kind in schema: " + kind);
        }
    }

    @SuppressWarnings("unchecked")
    private void validateChildContent(Map<String, Object> node, int depth, NodeCounter counter) {
        Object raw = node.get("content");
        if (raw == null) {
            return;
        }
        if (!(raw instanceof List<?> children)) {
            throw invalid("node content must be an array");
        }
        for (Object child : children) {
            if (!(child instanceof Map<?, ?> childMap)) {
                throw invalid("node content entries must be objects");
            }
            validateNode((Map<String, Object>) childMap, depth + 1, counter);
        }
    }

    private void validateTextNode(Map<String, Object> node) {
        Object textObj = node.get("text");
        if (!(textObj instanceof String)) {
            throw invalid("text node requires string text");
        }
        Object marksObj = node.get("marks");
        if (marksObj == null) {
            return;
        }
        if (!(marksObj instanceof List<?> marks)) {
            throw invalid("text marks must be an array");
        }
        for (Object markEntry : marks) {
            if (!(markEntry instanceof Map<?, ?> markMap)) {
                throw invalid("mark must be an object");
            }
            Object markTypeObj = markMap.get("type");
            if (!(markTypeObj instanceof String markType) || !schema.isAllowedMark(markType)) {
                throw invalid("disallowed mark type: " + markTypeObj);
            }
        }
    }

    private static BoardContentSchema loadSchema(ObjectMapper objectMapper) {
        try (InputStream in = ProseMirrorContentValidator.class.getClassLoader().getResourceAsStream(SCHEMA_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException(SCHEMA_RESOURCE + " not found on classpath");
            }
            return objectMapper.readValue(in, BoardContentSchema.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load " + SCHEMA_RESOURCE, e);
        }
    }

    private static ResponseStatusException invalid(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid content: " + message);
    }

    private static final class NodeCounter {
        private int count;
    }
}
