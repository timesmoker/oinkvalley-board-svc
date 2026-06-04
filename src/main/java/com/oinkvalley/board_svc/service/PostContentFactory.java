package com.oinkvalley.board_svc.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** plain text → TipTap {@code doc} JSON (프론트 Viewer·에디터와 호환). */
final class PostContentFactory {

    private PostContentFactory() {
    }

    static Map<String, Object> fromPlainText(String text, String sourceUrl) {
        return fromPlainText(text, sourceUrl, null);
    }

    static Map<String, Object> fromPlainText(String text, String sourceUrl, Map<String, Object> metadata) {
        List<Map<String, Object>> blocks = new ArrayList<>();
        if (text != null && !text.isBlank()) {
            blocks.add(paragraph(text.trim()));
        }
        if (sourceUrl != null && !sourceUrl.isBlank()) {
            blocks.add(linkParagraph(sourceUrl.trim(), "출처"));
        }
        if (blocks.isEmpty()) {
            blocks.add(paragraph(""));
        }
        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("type", "doc");
        doc.put("content", blocks);
        if (metadata != null && !metadata.isEmpty()) {
            doc.put("_bubblePal", metadata);
        }
        return doc;
    }

    private static Map<String, Object> paragraph(String text) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("type", "paragraph");
        node.put("content", List.of(textNode(text)));
        return node;
    }

    private static Map<String, Object> linkParagraph(String href, String label) {
        Map<String, Object> mark = new LinkedHashMap<>();
        mark.put("type", "link");
        Map<String, Object> attrs = new LinkedHashMap<>();
        attrs.put("href", href);
        attrs.put("target", "_blank");
        attrs.put("rel", "noopener noreferrer");
        mark.put("attrs", attrs);

        Map<String, Object> text = textNode(label);
        text.put("marks", List.of(mark));

        Map<String, Object> node = new LinkedHashMap<>();
        node.put("type", "paragraph");
        node.put("content", List.of(text));
        return node;
    }

    private static Map<String, Object> textNode(String text) {
        Map<String, Object> node = new LinkedHashMap<>();
        node.put("type", "text");
        node.put("text", text);
        return node;
    }
}
