package org.jmouse.web.parameters.support;

import org.jmouse.web.parameters.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RequestParametersTreeFlattener {

    public Map<String, List<String>> flatten(RequestParametersTree tree) {
        Map<String, List<String>> out = new LinkedHashMap<>();
        flattenObject(out, "", tree.root());
        return out;
    }

    private void flattenObject(Map<String, List<String>> out, String prefix, RequestParametersObjectNode objectNode) {
        for (Map.Entry<String, RequestParametersNode> entry : objectNode.fields().entrySet()) {
            String                key   = entry.getKey();
            RequestParametersNode value = entry.getValue();

            String nextPrefix = prefix.isEmpty() ? key : prefix + "[" + key + "]";
            flattenNode(out, nextPrefix, value);
        }
    }

    private void flattenNode(Map<String, List<String>> out, String path, RequestParametersNode node) {
        if (node == null) {
            // keep "path=" empty value? choose policy; MVP: emit nothing
            return;
        }

        switch (node.nodeType()) {
            case SCALAR -> putAll(out, path, ((RequestParametersScalarNode) node).values());
            case OBJECT -> flattenObject(out, path, (RequestParametersObjectNode) node);
            case ARRAY -> flattenArray(out, path, (RequestParametersArrayNode) node);
        }
    }

    private void flattenArray(Map<String, List<String>> out, String prefix, RequestParametersArrayNode arrayNode) {
        List<RequestParametersNode> items = arrayNode.items();

        // Policy: keep indices to preserve sparse arrays (ids[1]=444)
        for (int i = 0; i < items.size(); i++) {
            RequestParametersNode item = items.get(i);
            if (item == null) continue;

            String indexedPath = prefix + "[" + i + "]";
            flattenNode(out, indexedPath, item);
        }

        // Alternative policy (optional): emit append form "groups[]" for dense arrays
        // If you want it, add a strategy flag.
    }

    private static void putAll(Map<String, List<String>> out, String key, List<String> values) {
        List<String> list = out.computeIfAbsent(key, k -> new ArrayList<>());
        list.addAll(values);
    }
}
