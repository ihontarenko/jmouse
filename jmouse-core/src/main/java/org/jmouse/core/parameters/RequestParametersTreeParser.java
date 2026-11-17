package org.jmouse.core.parameters;

import org.jmouse.core.parameters.RequestParametersPathToken.ArrayAppendToken;
import org.jmouse.core.parameters.RequestParametersPathToken.ArrayIndexToken;
import org.jmouse.core.parameters.RequestParametersPathToken.PropertyNameToken;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 🌳 Names and their values built into one tree: each name read into its path ({@link RequestParametersPathParser}), each value
 * set at the place the path names — a record's field, a list's item by its index, a list's next item for {@code []}. How names
 * may be spelled, what a place two names disagree on becomes, what the root may be and which values say an empty place are its
 * {@link RequestParametersTreeOptions}; a name given twice holds both its values.
 */
public final class RequestParametersTreeParser {

    private final RequestParametersPathParser  pathParser = new RequestParametersPathParser();
    private final RequestParametersTreeOptions options;

    /** A parser for a request's parameters ({@link RequestParametersTreeOptions#defaults()}). */
    public RequestParametersTreeParser() {
        this(RequestParametersTreeOptions.defaults());
    }

    public RequestParametersTreeParser(RequestParametersTreeOptions options) {
        this.options = options;
    }

    /**
     * The tree the names make; an empty record where there are none.
     *
     * @throws IllegalArgumentException where the options refuse a name's spelling, or two names that disagree on a place
     */
    public RequestParametersTree parse(Map<String, String[]> parameters) {
        RequestParametersNode root = null;
        for (Map.Entry<String, String[]> parameter : parameters.entrySet()) {
            String                           name = parameter.getKey();
            List<RequestParametersPathToken> path = pathParser.parse(name, options.spelling());
            if (path.isEmpty() || options.root() == RequestParametersTreeOptions.Root.RECORD && !(path.getFirst() instanceof PropertyNameToken)) {
                continue;
            }
            List<String> values = parameter.getValue() == null ? List.of() : Arrays.asList(parameter.getValue());
            root = place(root, path, 0, values, name);
        }
        return new RequestParametersTree(root == null ? new RequestParametersObjectNode(new LinkedHashMap<>()) : root);
    }

    /** The node at a depth of a name's path, with the name's values set at its end. */
    private RequestParametersNode place(RequestParametersNode node, List<RequestParametersPathToken> path, int depth, List<String> values, String name) {
        if (depth == path.size()) {
            return valueAt(node, values, name);
        }
        switch (path.get(depth)) {
            case PropertyNameToken(String propertyName) -> {
                RequestParametersObjectNode record = recordAt(node, name);
                record.fields().put(propertyName, place(record.fields().get(propertyName), path, depth + 1, values, name));
                return record;
            }
            case ArrayIndexToken(int index) -> {
                RequestParametersArrayNode list = listAt(node, name);
                while (list.items().size() <= index) {
                    list.items().add(null);
                }
                list.items().set(index, place(list.items().get(index), path, depth + 1, values, name));
                return list;
            }
            case ArrayAppendToken ignored -> {
                RequestParametersArrayNode list = listAt(node, name);
                list.items().add(place(null, path, depth + 1, values, name));
                return list;
            }
        }
    }

    /** What stands where a name ends: its values — or, where they say one, an empty record or list that gives way to more. */
    private RequestParametersNode valueAt(RequestParametersNode node, List<String> values, String name) {
        RequestParametersTreeOptions.Emptiness emptiness = options.emptiness();
        if (emptiness.saysRecord(values)) {
            return recordAt(node, name);
        }
        if (emptiness.saysList(values)) {
            return listAt(node, name);
        }
        if (node == null) {
            return new RequestParametersScalarNode(new ArrayList<>(values));
        }
        if (node instanceof RequestParametersScalarNode(List<String> held)) {
            held.addAll(values);
            return node;
        }
        return disagreed(name, "is a value where another name holds more", () -> new RequestParametersScalarNode(new ArrayList<>(values)));
    }

    private RequestParametersObjectNode recordAt(RequestParametersNode node, String name) {
        return switch (node) {
            case null -> new RequestParametersObjectNode(new LinkedHashMap<>());
            case RequestParametersObjectNode record -> record;
            default -> disagreed(name, "says a place is a record that another name says is not",
                                 () -> new RequestParametersObjectNode(new LinkedHashMap<>()));
        };
    }

    private RequestParametersArrayNode listAt(RequestParametersNode node, String name) {
        return switch (node) {
            case null -> new RequestParametersArrayNode(new ArrayList<>());
            case RequestParametersArrayNode list -> list;
            default -> disagreed(name, "says a place is a list that another name says is not", () -> new RequestParametersArrayNode(new ArrayList<>()));
        };
    }

    /** A place two names disagree on: the later name's shape, or a refusal, as the options say. */
    private <N extends RequestParametersNode> N disagreed(String name, String why, Supplier<N> later) {
        if (options.conflict() == RequestParametersTreeOptions.Conflict.REFUSE) {
            throw new IllegalArgumentException("'" + name + "' " + why + ".");
        }
        return later.get();
    }
}
