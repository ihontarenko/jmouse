package org.jmouse.parallax.script.engine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JSON text and the tree it stands for, read and written by Jackson: objects as ordered maps, arrays as lists, texts,
 * numbers as {@code Double}, truth values and null. A symbol's drawing is the one JSON a definition holds. Writing
 * keeps a map's order, so two equal trees are equal texts; a whole {@code Integer} or {@code Long} is printed as it is.
 */
final class JsonText {

    /** Text after the document is refused, not ignored. */
    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();

    private JsonText() {
    }

    /** The tree a JSON text stands for, every number a {@code Double}. */
    static Object read(String json) {
        try {
            return tree(MAPPER.readTree(json));
        } catch (JsonProcessingException invalid) {
            throw new IllegalArgumentException("Not valid JSON: " + invalid.getOriginalMessage() + ".", invalid);
        }
    }

    /** A tree as JSON text: maps, lists, texts, numbers, truth values and null. */
    static String write(Object value) {
        try {
            return MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException unwritable) {
            throw new IllegalArgumentException("JSON holds objects, arrays, texts, numbers and truth values: "
                    + unwritable.getOriginalMessage(), unwritable);
        }
    }

    private static Object tree(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return null;
        }
        if (node.isObject()) {
            Map<String, Object> object = new LinkedHashMap<>();
            node.properties().forEach(entry -> object.put(entry.getKey(), tree(entry.getValue())));
            return object;
        }
        if (node.isArray()) {
            List<Object> array = new ArrayList<>(node.size());
            node.forEach(element -> array.add(tree(element)));
            return array;
        }
        if (node.isNumber()) {
            return node.doubleValue();
        }
        if (node.isBoolean()) {
            return node.booleanValue();
        }
        return node.textValue();
    }
}
