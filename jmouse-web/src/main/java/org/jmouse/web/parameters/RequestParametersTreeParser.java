package org.jmouse.web.parameters;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RequestParametersTreeParser {

    private final RequestParametersPathParser pathParser = new RequestParametersPathParser();

    public RequestParametersTree parse(Map<String, String[]> parameters) {
        RequestParametersObjectNode root = new RequestParametersObjectNode(new LinkedHashMap<>());

        for (Map.Entry<String, String[]> entry : parameters.entrySet()) {
            String parameterName = entry.getKey();
            String[] rawValues = entry.getValue() == null ? new String[0] : entry.getValue();

            List<RequestParametersPathToken> pathTokens = pathParser.parse(parameterName);
            if (pathTokens.isEmpty()) {
                continue;
            }

            insert(root, pathTokens, toList(rawValues));
        }

        return new RequestParametersTree(root);
    }

    private void insert(RequestParametersObjectNode root,
                        List<RequestParametersPathToken> pathTokens,
                        List<String> values) {

        RequestParametersPathToken first = pathTokens.getFirst();
        if (!(first instanceof RequestParametersPathToken.PropertyNameToken(String propertyName))) {
            return;
        }

        RequestParametersNode existing = root.fields().get(propertyName);
        RequestParametersNode updated  = insertInto(existing, pathTokens, 0, values);

        root.fields().put(propertyName, updated);
    }

    private RequestParametersNode insertInto(RequestParametersNode currentNode,
                                             List<RequestParametersPathToken> pathTokens,
                                             int position,
                                             List<String> values) {

        RequestParametersPathToken currentToken = pathTokens.get(position);
        boolean                    isLastToken  = position == pathTokens.size() - 1;

        if (currentToken instanceof RequestParametersPathToken.PropertyNameToken(String propertyName)) {
            if (isLastToken) {
                return mergeScalarValues(currentNode, values);
            }

            RequestParametersObjectNode objectNode = toObjectNode(currentNode);

            RequestParametersPathToken nextToken = pathTokens.get(position + 1);

            if (nextToken instanceof RequestParametersPathToken.PropertyNameToken(String name)) {
                RequestParametersNode child    = objectNode.fields().get(name);
                RequestParametersNode newChild = insertInto(child, pathTokens, position + 1, values);
                objectNode.fields().put(name, newChild);
                return objectNode;
            }

            if (nextToken instanceof RequestParametersPathToken.ArrayIndexToken || nextToken instanceof RequestParametersPathToken.ArrayAppendToken) {
                // propertyName -> next token is array access, so the property value is an array node
                RequestParametersNode childArrayNode    = objectNode.fields().get(propertyName);
                RequestParametersNode newChildArrayNode = insertInto(childArrayNode, pathTokens, position + 1, values);
                objectNode.fields().put(propertyName, newChildArrayNode);
                return objectNode;
            }

            return objectNode;
        }

        if (currentToken instanceof RequestParametersPathToken.ArrayIndexToken arrayIndexToken) {
            RequestParametersArrayNode arrayNode = toArrayNode(currentNode);
            int                        index     = arrayIndexToken.index();

            ensureArraySize(arrayNode.items(), index + 1);

            if (isLastToken) {
                RequestParametersNode item = arrayNode.items().get(index);
                arrayNode.items().set(index, mergeScalarValues(item, values));
                return arrayNode;
            }

            RequestParametersNode item = arrayNode.items().get(index);
            if (item == null) {
                item = new RequestParametersObjectNode(new LinkedHashMap<>());
            }

            RequestParametersNode newItem = insertInto(item, pathTokens, position + 1, values);
            arrayNode.items().set(index, newItem);
            return arrayNode;
        }

        if (currentToken instanceof RequestParametersPathToken.ArrayAppendToken) {
            RequestParametersArrayNode arrayNode = toArrayNode(currentNode);

            if (isLastToken) {
                arrayNode.items().add(new RequestParametersScalarNode(new ArrayList<>(values)));
                return arrayNode;
            }

            RequestParametersNode newItem = insertInto(new RequestParametersObjectNode(new LinkedHashMap<>()),
                                                       pathTokens,
                                                       position + 1,
                                                       values);

            arrayNode.items().add(newItem);
            return arrayNode;
        }

        return currentNode;
    }

    private static RequestParametersNode mergeScalarValues(RequestParametersNode existing, List<String> values) {
        if (existing == null) {
            return new RequestParametersScalarNode(new ArrayList<>(values));
        }
        if (existing instanceof RequestParametersScalarNode scalarNode) {
            scalarNode.values().addAll(values);
            return scalarNode;
        }
        // Conflict policy (MVP): overwrite non-scalar with scalar.
        return new RequestParametersScalarNode(new ArrayList<>(values));
    }

    private static RequestParametersObjectNode toObjectNode(RequestParametersNode node) {
        if (node instanceof RequestParametersObjectNode objectNode) {
            return objectNode;
        }
        return new RequestParametersObjectNode(new LinkedHashMap<>());
    }

    private static RequestParametersArrayNode toArrayNode(RequestParametersNode node) {
        if (node instanceof RequestParametersArrayNode arrayNode) {
            return arrayNode;
        }
        return new RequestParametersArrayNode(new ArrayList<>());
    }

    private static void ensureArraySize(List<RequestParametersNode> items, int size) {
        while (items.size() < size) {
            items.add(null);
        }
    }

    private static List<String> toList(String[] rawValues) {
        List<String> out = new ArrayList<>(rawValues.length);
        for (String rawValue : rawValues) {
            out.add(rawValue);
        }
        return out;
    }
}
