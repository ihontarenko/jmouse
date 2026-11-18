package org.jmouse.cad.live;

import org.jmouse.json.reading.TreeAccess;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

/** A Jackson tree as the structural reading walks it, a node read whole by the session's mapper. */
final class JacksonTreeAccess implements TreeAccess<JsonNode> {

    private final ObjectMapper mapper;

    JacksonTreeAccess(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Optional<JsonNode> member(JsonNode node, String name) {
        return node.isObject() && node.has(name) ? Optional.of(node.get(name)) : Optional.empty();
    }

    @Override
    public Optional<JsonNode> element(JsonNode node, int index) {
        return node.isArray() && index >= 0 && index < node.size() ? Optional.of(node.get(index)) : Optional.empty();
    }

    @Override
    public int size(JsonNode node) {
        return node.isArray() ? node.size() : -1;
    }

    @Override
    public Iterable<String> names(JsonNode node) {
        return node.isObject() ? node.propertyNames() : List.of();
    }

    @Override
    public Object read(JsonNode node, Type type) {
        return mapper.treeToValue(node, mapper.constructType(type));
    }
}
