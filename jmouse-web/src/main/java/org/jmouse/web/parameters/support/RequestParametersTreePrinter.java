package org.jmouse.web.parameters.support;

import org.jmouse.web.parameters.*;

import java.util.List;
import java.util.Map;

public final class RequestParametersTreePrinter {

    private RequestParametersTreePrinter() {}

    public static String toPrettyString(RequestParametersTree tree) {
        StringBuilder out = new StringBuilder();
        printNode(out, tree.root(), 0);
        return out.toString();
    }

    private static void printNode(StringBuilder out, RequestParametersNode node, int depth) {
        if (node == null) {
            out.append("null");
            return;
        }

        switch (node.nodeType()) {
            case OBJECT -> printObject(out, (RequestParametersObjectNode) node, depth);
            case ARRAY -> printArray(out, (RequestParametersArrayNode) node, depth);
            case SCALAR -> printScalar(out, (RequestParametersScalarNode) node);
        }
    }

    private static void printObject(StringBuilder out, RequestParametersObjectNode objectNode, int depth) {
        out.append("{\n");
        int i = 0;
        for (Map.Entry<String, RequestParametersNode> entry : objectNode.fields().entrySet()) {
            indent(out, depth + 1);
            out.append(entry.getKey()).append(": ");
            printNode(out, entry.getValue(), depth + 1);
            if (++i < objectNode.fields().size()) out.append(",");
            out.append("\n");
        }
        indent(out, depth);
        out.append("}");
    }

    private static void printArray(StringBuilder out, RequestParametersArrayNode arrayNode, int depth) {
        out.append("[\n");
        List<RequestParametersNode> items = arrayNode.items();
        for (int i = 0; i < items.size(); i++) {
            indent(out, depth + 1);
            printNode(out, items.get(i), depth + 1);
            if (i + 1 < items.size()) out.append(",");
            out.append("\n");
        }
        indent(out, depth);
        out.append("]");
    }

    private static void printScalar(StringBuilder out, RequestParametersScalarNode scalarNode) {
        List<String> values = scalarNode.values();
        if (values.size() == 1) {
            out.append('"').append(values.get(0)).append('"');
            return;
        }
        out.append(values);
    }

    private static void indent(StringBuilder out, int depth) {
        for (int i = 0; i < depth; i++) out.append("  ");
    }
}
