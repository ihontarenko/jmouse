package org.jmouse.dom.meterializer.hooks;

import org.jmouse.core.access.AccessorWrapper;
import org.jmouse.core.access.ObjectAccessor;
import org.jmouse.core.access.ValueNavigator;
import org.jmouse.dom.Node;
import org.jmouse.meterializer.RenderingExecution;
import org.jmouse.meterializer.RenderingRequest;
import org.jmouse.meterializer.SubmissionState;
import org.jmouse.meterializer.hooks.RenderingHook;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Populates option selection flags ({@code option.checked}) for checkbox/radio controls
 * using {@link SubmissionState}. ✅
 *
 * <p>
 * This hook runs before template resolution and mutates the model tree by traversing it
 * via {@link ObjectAccessor}. For each encountered "control-like" object (having both
 * {@code name} and {@code options}), it compares submitted values against option keys
 * and sets {@code checked=true/false} on each option entry.
 * </p>
 *
 * <h3>Expected model contract (path-based)</h3>
 * <ul>
 *   <li>Control name: {@code "name"}</li>
 *   <li>Options list/collection: {@code "options"}</li>
 *   <li>Option key field: {@code "key"}</li>
 *   <li>Option checked flag: {@code "checked"} (written by this hook)</li>
 * </ul>
 *
 * <h3>Submission contract</h3>
 * <p>
 * Submission is expected to be present in {@link RenderingRequest#attributes()}
 * under {@link SubmissionState#REQUEST_ATTRIBUTE}. The hook reads:
 * </p>
 *
 * <pre>{@code
 * Object submitted = submission.values().get(controlName);
 * }</pre>
 *
 * <p>
 * The submitted value may be:
 * </p>
 * <ul>
 *     <li>a single {@link String}</li>
 *     <li>a {@link Collection} of values</li>
 *     <li>an array</li>
 *     <li>any scalar value (stringified)</li>
 * </ul>
 *
 * <h3>Traversal</h3>
 * <p>
 * After processing the current node, the hook attempts to continue recursively through
 * common container paths (if present and list/collection-like):
 * </p>
 * <ul>
 *     <li>{@code content}</li>
 *     <li>{@code children}</li>
 *     <li>{@code controls}</li>
 *     <li>{@code nodes}</li>
 * </ul>
 *
 * <p>
 * ⚠ Note: this hook mutates the model (sets {@code checked} on options).
 * Ensure your model objects are intended to be writable via {@link ObjectAccessor#set(String, Object)}.
 * </p>
 */
public final class OptionSelectionHook implements RenderingHook<Node> {

    /**
     * Runs late in the hook chain (default: 900) so other model-enrichment hooks can run first.
     *
     * @return hook order
     */
    @Override
    public int order() {
        return 900;
    }

    /**
     * Extracts {@link SubmissionState} from request attributes and attaches {@code checked} flags
     * across the model tree.
     *
     * @param blueprintKey template key being resolved
     * @param data original model object passed to render
     * @param request rendering request (contains attributes)
     * @param execution rendering execution context
     */
    @Override
    public void beforeTemplateResolve(String blueprintKey, Object data, RenderingRequest request, RenderingExecution execution) {
        SubmissionState submission = (SubmissionState) request.attributes().get(SubmissionState.REQUEST_ATTRIBUTE);

        if (submission == null) {
            return;
        }

        ObjectAccessor root = execution.rootAccessor();
        attachRecursively(root, submission, execution);
    }

    /**
     * Traverses the given accessor and:
     * <ul>
     *     <li>detects "control-like" structures (has {@code name} + {@code options})</li>
     *     <li>computes selected keys based on {@link SubmissionState}</li>
     *     <li>sets {@code option.checked} for each option</li>
     * </ul>
     *
     * @param node current object accessor
     * @param submission submission state
     * @param execution execution context
     */
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

    /**
     * Walks a nested list/collection at the given path if present and recurses into its elements.
     *
     * @param node current accessor
     * @param path property name/path to navigate
     * @param submission submission state
     * @param execution execution context
     */
    private void walkIfExists(ObjectAccessor node, String path, SubmissionState submission, RenderingExecution execution) {
        ObjectAccessor  accessor = safeNavigate(node, path, execution);
        AccessorWrapper wrapper  = execution.accessorWrapper();

        if (accessor == null) {
            return;
        }

        if (accessor.isList() || accessor.isCollection()) {
            for (Object key : accessor.keySet()) {
                attachRecursively(wrapper.wrapIfNecessary(accessor.get(key)), submission, execution);
            }
        }
    }

    /**
     * Safe navigation helper that returns {@code null} on any navigation failure.
     *
     * <p>
     * Uses {@link RenderingExecution#valueNavigator()} to navigate from {@code accessor} by {@code path}.
     * If the returned value is not an {@link ObjectAccessor}, it is wrapped via
     * {@link RenderingExecution#accessorWrapper()}.
     * </p>
     *
     * @param accessor starting accessor
     * @param path property path
     * @param execution execution context
     * @return resulting accessor or {@code null} if navigation fails
     */
    private ObjectAccessor safeNavigate(ObjectAccessor accessor, String path, RenderingExecution execution) {
        AccessorWrapper wrapper   = execution.accessorWrapper();
        ValueNavigator  navigator = execution.valueNavigator();
        try {
            return wrapper.wrapIfNecessary(navigator.navigate(accessor, path));
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Unwraps scalar accessors to their underlying value; non-scalars return the accessor itself.
     *
     * @param accessor accessor to unwrap
     * @return unwrapped scalar value or accessor
     */
    private Object unwrap(ObjectAccessor accessor) {
        Object unwrapped = accessor;
        if (accessor.isScalar()) {
            unwrapped = accessor.unwrap();
        }
        return unwrapped;
    }

    /**
     * Converts a submitted value into a set of string keys for comparison with option keys.
     *
     * @param submitted submitted value (string/collection/array/scalar)
     * @return set of stringified values (never null)
     */
    private Set<String> toStringSet(Object submitted) {
        Set<String> result = new HashSet<>();

        switch (submitted) {
            case null -> {
                return result;
            }
            case String string -> {
                result.add(string);
                return result;
            }
            case Collection<?> collection -> {
                for (Object value : collection) {
                    result.add(String.valueOf(value));
                }
                return result;
            }
            default -> { }
        }

        if (submitted.getClass().isArray()) {
            int length = Array.getLength(submitted);
            for (int i = 0; i < length; i++) {
                result.add(String.valueOf(Array.get(submitted, i)));
            }
            return result;
        }

        result.add(String.valueOf(submitted));

        return result;
    }
}