package org.jmouse.web.parameters;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RequestParametersJavaStructureConverter {

    private final RequestParametersJavaStructureOptions options;

    public RequestParametersJavaStructureConverter(RequestParametersJavaStructureOptions options) {
        this.options = options;
    }

    public Object toJavaObject(RequestParametersTree tree) {
        return convertNode(tree.root());
    }

    public Map<String, Object> toMap(RequestParametersTree tree) {
        Object converted = toJavaObject(tree);

        if (converted instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> typed = (Map<String, Object>) map;
            return typed;
        }

        return Map.of();
    }

    private Object convertNode(RequestParametersNode node) {
        if (node == null) return null;

        return switch (node.nodeType()) {
            case OBJECT -> convertObject((RequestParametersObjectNode) node);
            case ARRAY -> convertArray((RequestParametersArrayNode) node);
            case SCALAR -> convertScalar((RequestParametersScalarNode) node);
        };
    }

    private Map<String, Object> convertObject(RequestParametersObjectNode objectNode) {
        Map<String, Object> map = new LinkedHashMap<>();

        for (Map.Entry<String, RequestParametersNode> entry : objectNode.fields().entrySet()) {
            map.put(entry.getKey(), convertNode(entry.getValue()));
        }

        return map;
    }

    private Object convertArray(RequestParametersArrayNode arrayNode) {
        List<Object> list = new ArrayList<>();

        for (RequestParametersNode item : arrayNode.items()) {
            if (item == null) {
                if (!options.dropNullArrayItems()) {
                    list.add(null);
                }
                continue;
            }
            list.add(convertNode(item));
        }

        if (options.unwrapSingleElementArray() && list.size() == 1) {
            return list.getFirst();
        }

        return list;
    }

    private Object convertScalar(RequestParametersScalarNode scalarNode) {
        List<String> values = scalarNode.values();

        if (options.unwrapSingleScalarValue() && values.size() == 1) {
            return values.getFirst();
        }

        return List.copyOf(values);
    }
}
