package org.jmouse.core.parameters;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 🔁 A tree as plain Java and back: a record a {@link Map}, a list a {@link List}, a value its text — one value alone, or several
 * as a list, as the {@link RequestParametersJavaStructureOptions} say. Plain Java becomes a tree with every value as its text: a
 * number as it is written, without the zeros a decimal trails.
 */
public final class RequestParametersJavaStructureConverter {

    private final RequestParametersJavaStructureOptions options;

    public RequestParametersJavaStructureConverter(RequestParametersJavaStructureOptions options) {
        this.options = options;
    }

    public Object toJavaObject(RequestParametersTree tree) {
        return convertNode(tree.root());
    }

    /**
     * The tree plain Java makes: a {@link Map} a record, a {@link Collection} a list, null a value that is null, anything else its
     * text.
     *
     * @throws IllegalArgumentException where the Java is not a record or a list: a value is a parameter only under a name
     */
    public RequestParametersTree toTree(Object java) {
        return new RequestParametersTree(nodeOf(java));
    }

    private static RequestParametersNode nodeOf(Object java) {
        return switch (java) {
            case Map<?, ?> record -> {
                Map<String, RequestParametersNode> fields = new LinkedHashMap<>();
                record.forEach((name, value) -> fields.put(String.valueOf(name), nodeOf(value)));
                yield new RequestParametersObjectNode(fields);
            }
            case Collection<?> list -> {
                List<RequestParametersNode> items = new ArrayList<>();
                list.forEach(item -> items.add(nodeOf(item)));
                yield new RequestParametersArrayNode(items);
            }
            case null -> new RequestParametersScalarNode(new ArrayList<>(Collections.singletonList(null)));
            default -> new RequestParametersScalarNode(new ArrayList<>(List.of(textOf(java))));
        };
    }

    /** A value as text: a decimal without the zeros it trails, anything else as it says itself. */
    private static String textOf(Object value) {
        return switch (value) {
            case Double number -> BigDecimal.valueOf(number).stripTrailingZeros().toPlainString();
            case Float number -> new BigDecimal(number.toString()).stripTrailingZeros().toPlainString();
            case BigDecimal number -> number.stripTrailingZeros().toPlainString();
            default -> String.valueOf(value);
        };
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
        return switch (node) {
            case null -> null;
            case RequestParametersObjectNode objectNode -> convertObject(objectNode);
            case RequestParametersArrayNode arrayNode -> convertArray(arrayNode);
            case RequestParametersScalarNode scalarNode -> convertScalar(scalarNode);
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

        // A value that is null is a value: kept, where List.copyOf would refuse it.
        return Collections.unmodifiableList(new ArrayList<>(values));
    }
}
