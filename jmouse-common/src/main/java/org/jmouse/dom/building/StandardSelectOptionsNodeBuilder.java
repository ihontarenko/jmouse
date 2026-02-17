package org.jmouse.dom.building;

import org.jmouse.core.Verify;
import org.jmouse.dom.Node;
import org.jmouse.dom.TagName;
import org.jmouse.dom.node.ElementNode;
import org.jmouse.dom.node.TextNode;

import java.util.Iterator;
import java.util.Map;

/**
 * Builds {@code <select><option/></select>} from an iterable of maps.
 *
 * <p>Expected map keys (preferred):</p>
 * <ul>
 *   <li>{@code "value"} option value</li>
 *   <li>{@code "label"} option label</li>
 * </ul>
 *
 * <p>Fallback: if preferred keys are missing, the first entry is used.</p>
 */
public final class StandardSelectOptionsNodeBuilder implements NodeBuilder {

    public static final String PURPOSE_SELECT_OPTIONS = "selectOptions";

    @Override
    public boolean supports(NodeBuildRequest request, NodeBuildContext context) {
        Verify.nonNull(request, "request");

        boolean purposeMatches = request.purpose().map(PURPOSE_SELECT_OPTIONS::equals).orElse(false);
        boolean tagMatches     = request.preferredTagName().map(tag -> tag == TagName.SELECT).orElse(false);

        if (!purposeMatches && !tagMatches) {
            return false;
        }

        Object value = request.value();
        if (!(value instanceof Iterable<?>)) {
            return false;
        }

        for (Object element : (Iterable<?>) value) {
            if (!(element instanceof Map<?, ?>)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public Node build(NodeBuildRequest request, NodeBuildContext context) {
        Object value = context.requireNonNull(request.value(), "value");

        ElementNode select = new ElementNode(TagName.SELECT);

        for (Object element : (Iterable<?>) value) {
            @SuppressWarnings("unchecked")
            Map<Object, Object> map = (Map<Object, Object>) element;
            select.append(createOption(map));
        }

        return select;
    }

    private Node createOption(Map<Object, Object> map) {
        String optionValue = readString(map, "value");
        String optionLabel = readString(map, "label");

        if (optionValue == null || optionLabel == null) {
            Map.Entry<Object, Object> firstEntry = firstEntry(map);
            optionValue = optionValue != null ? optionValue : String.valueOf(firstEntry.getKey());
            optionLabel = optionLabel != null ? optionLabel : String.valueOf(firstEntry.getValue());
        }

        ElementNode option = new ElementNode(TagName.OPTION);
        option.addAttribute("value", optionValue);
        option.append(new TextNode(optionLabel));
        return option;
    }

    private String readString(Map<Object, Object> map, String key) {
        Object value = map.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private Map.Entry<Object, Object> firstEntry(Map<Object, Object> map) {
        Iterator<Map.Entry<Object, Object>> iterator = map.entrySet().iterator();
        if (!iterator.hasNext()) {
            throw new IllegalArgumentException("Map must contain at least one entry for option generation.");
        }
        return iterator.next();
    }

    @Override
    public int order() {
        return 1000;
    }
}
