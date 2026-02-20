package org.jmouse.dom.meterializer.hooks;

import org.jmouse.common.dom.Node;
import org.jmouse.core.access.ObjectAccessor;
import org.jmouse.meterializer.RenderingExecution;
import org.jmouse.meterializer.RenderingRequest;
import org.jmouse.meterializer.SubmissionState;
import org.jmouse.meterializer.hooks.RenderingHook;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Populates "option.checked" for checkbox and radio option lists based on {@link SubmissionState}.
 *
 * <p>Contract expectations (path-based):</p>
 * <ul>
 *   <li>Control name: "name"</li>
 *   <li>Options list: "options"</li>
 *   <li>Option key: "key"</li>
 *   <li>Option checked flag: "checked"</li>
 * </ul>
 */
public final class OptionSelectionHook implements RenderingHook<Node> {

    @Override
    public int order() {
        return 900;
    }

    @Override
    public void beforeTemplateResolve(String blueprintKey, Object data, RenderingRequest request, RenderingExecution execution) {
        SubmissionState submission = (SubmissionState) request.attributes().get(SubmissionState.REQUEST_ATTRIBUTE);

        if (submission == null) {
            return;
        }

        ObjectAccessor root = execution.rootAccessor();
        attachRecursively(root, submission, execution);
    }

    private void attachRecursively(ObjectAccessor node, SubmissionState submission, RenderingExecution execution) {
        if (node == null) return;

        // If this looks like a control: has "name" and "options"
        ObjectAccessor nameAccessor    = safeNavigate(node, "name", execution);
        ObjectAccessor optionsAccessor = safeNavigate(node, "options", execution);

        if (nameAccessor != null && optionsAccessor != null && (optionsAccessor.isList() || optionsAccessor.isCollection())) {
            String controlName = String.valueOf(unwrap(nameAccessor));
            Object submitted   = submission.values().get(controlName);

            Set<String> selected = toStringSet(submitted);

            // options: iterate keys [0..n] or collection keySet
            for (Object optionKey : optionsAccessor.keySet()) {
                Object         optionValue = optionsAccessor.get(optionKey);
                ObjectAccessor option      = execution.accessorWrapper().wrapIfNecessary(optionValue);
                ObjectAccessor keyAccessor = safeNavigate(option, "key", execution);
                String         keyString   = keyAccessor == null ? null : String.valueOf(unwrap(keyAccessor));

                boolean checked = keyString != null && selected.contains(keyString);

                option.set("checked", checked);
            }
        }

        // Walk children collections if present (common patterns)
        walkIfExists(node, "content", submission, execution);
        walkIfExists(node, "children", submission, execution);
        walkIfExists(node, "controls", submission, execution);
        walkIfExists(node, "nodes", submission, execution);
    }

    private void walkIfExists(ObjectAccessor node, String path, SubmissionState submission, RenderingExecution execution) {
        ObjectAccessor childAccessor = safeNavigate(node, path, execution);

        if (childAccessor == null) {
            return;
        }

        if (childAccessor.isList() || childAccessor.isCollection()) {
            for (Object key : childAccessor.keySet()) {
                Object child = childAccessor.get(key);
                attachRecursively(execution.accessorWrapper().wrapIfNecessary(child), submission, execution);
            }
        }
    }

    private ObjectAccessor safeNavigate(ObjectAccessor accessor, String path, RenderingExecution execution) {
        try {
            Object value = execution.valueNavigator().navigate(accessor, path);
            return value instanceof ObjectAccessor objectAccessor ? objectAccessor : execution.accessorWrapper().wrapIfNecessary(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Object unwrap(ObjectAccessor accessor) {
        Object unwrapped = accessor;
        if (accessor.isScalar()) {
            unwrapped = accessor.unwrap();
        }
        return unwrapped;
    }

    private Set<String> toStringSet(Object submitted) {
        Set<String> result = new HashSet<>();
        if (submitted == null) return result;

        if (submitted instanceof String s) {
            result.add(s);
            return result;
        }

        if (submitted instanceof Collection<?> c) {
            for (Object v : c) result.add(String.valueOf(v));
            return result;
        }

        if (submitted.getClass().isArray()) {
            int n = Array.getLength(submitted);
            for (int i = 0; i < n; i++) {
                result.add(String.valueOf(Array.get(submitted, i)));
            }
            return result;
        }

        result.add(String.valueOf(submitted));
        return result;
    }
}
