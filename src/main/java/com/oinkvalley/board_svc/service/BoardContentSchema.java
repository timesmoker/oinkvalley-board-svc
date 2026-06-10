package com.oinkvalley.board_svc.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

/** Loaded from classpath {@code content-schema.json} (source: repo {@code board/content-schema.json}). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BoardContentSchema(
        String rootType,
        int maxNodes,
        int maxDepth,
        Map<String, String> nodes,
        List<String> marks) {

    public String nodeKind(String type) {
        return nodes.get(type);
    }

    public boolean isAllowedMark(String markType) {
        return marks.contains(markType);
    }
}
