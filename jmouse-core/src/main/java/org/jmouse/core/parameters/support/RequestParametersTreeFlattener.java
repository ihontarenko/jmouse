package org.jmouse.core.parameters.support;

import org.jmouse.core.parameters.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 🪜 A tree laid flat again: each value under the name its path is written by ({@link RequestParametersPathWriter}), so parsing
 * the names gives the tree back. A list keeps its items' indices, a sparse one too ({@code ids[1]}). An empty record or list is
 * named with the value that says it ({@link RequestParametersTreeOptions.Emptiness}); with none, it is left out, as is a place a
 * sparse list leaves empty.
 */
public final class RequestParametersTreeFlattener {

    private final RequestParametersPathWriter            pathWriter = new RequestParametersPathWriter();
    private final RequestParametersTreeOptions.Emptiness emptiness;

    /** A flattener that leaves an empty record or list out. */
    public RequestParametersTreeFlattener() {
        this(RequestParametersTreeOptions.Emptiness.NONE);
    }

    public RequestParametersTreeFlattener(RequestParametersTreeOptions.Emptiness emptiness) {
        this.emptiness = emptiness;
    }

    /**
     * The names and values the tree is made of, in its order.
     *
     * @throws IllegalArgumentException where a record's field has a name no path can hold — a bracket in it, or nothing
     */
    public Map<String, List<String>> flatten(RequestParametersTree tree) {
        Map<String, List<String>> flattened = new LinkedHashMap<>();
        flattenNode(flattened, new ArrayList<>(), tree.root());
        return flattened;
    }

    private void flattenNode(Map<String, List<String>> flattened, List<RequestParametersPathToken> path, RequestParametersNode node) {
        switch (node) {
            case null -> {
                // A place a sparse list leaves empty: nothing stands there to be named.
            }
            case RequestParametersScalarNode scalarNode ->
                    flattened.computeIfAbsent(pathWriter.write(path), name -> new ArrayList<>()).addAll(scalarNode.values());
            case RequestParametersObjectNode objectNode when objectNode.fields().isEmpty() -> nameEmpty(flattened, path, emptiness.record());
            case RequestParametersObjectNode objectNode -> objectNode.fields().forEach((name, value) -> {
                path.add(new RequestParametersPathToken.PropertyNameToken(name));
                flattenNode(flattened, path, value);
                path.removeLast();
            });
            case RequestParametersArrayNode arrayNode when arrayNode.items().isEmpty() -> nameEmpty(flattened, path, emptiness.list());
            case RequestParametersArrayNode arrayNode -> {
                List<RequestParametersNode> items = arrayNode.items();
                for (int index = 0; index < items.size(); index++) {
                    path.add(new RequestParametersPathToken.ArrayIndexToken(index));
                    flattenNode(flattened, path, items.get(index));
                    path.removeLast();
                }
            }
        }
    }

    /** An empty record or list, named with the value that says it — where one does. */
    private void nameEmpty(Map<String, List<String>> flattened, List<RequestParametersPathToken> path, String value) {
        if (value != null) {
            flattened.put(pathWriter.write(path), new ArrayList<>(List.of(value)));
        }
    }
}
