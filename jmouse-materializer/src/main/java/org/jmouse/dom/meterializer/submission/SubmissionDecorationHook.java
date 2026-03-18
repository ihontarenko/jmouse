package org.jmouse.dom.meterializer.submission;

import org.jmouse.core.Visitor;
import org.jmouse.dom.Node;
import org.jmouse.dom.NodeType;
import org.jmouse.dom.TagName;
import org.jmouse.dom.node.ElementNode;
import org.jmouse.dom.node.TextNode;
import org.jmouse.meterializer.RenderingExecution;
import org.jmouse.meterializer.SubmissionState;
import org.jmouse.meterializer.hooks.RenderingHook;

import java.util.*;

import static org.jmouse.meterializer.SubmissionState.REQUEST_ATTRIBUTE;

/**
 * {@link RenderingHook} that decorates form controls using submission state. 🧩
 *
 * <p>
 * After materialization, this hook reads {@link SubmissionState} from request
 * attributes and applies:
 * </p>
 * <ul>
 *     <li>submitted values to supported controls</li>
 *     <li>validation error styling</li>
 *     <li>invalid feedback nodes with error messages</li>
 * </ul>
 *
 * <p>
 * Value binding is delegated to registered {@link ControlValueApplier appliers}.
 * Error placement is controlled by {@link TargetSelector}, while field keys
 * are resolved through {@link FieldKeyResolver}.
 * </p>
 */
public final class SubmissionDecorationHook implements RenderingHook<Node> {

    private final FieldKeyResolver          keyResolver;
    private final TargetSelector            targetSelector;
    private final List<ControlValueApplier> valueAppliers;

    /**
     * Creates hook with required submission decoration components.
     *
     * @param targetSelector selector for error target nodes
     * @param keyResolver    resolver for value and error keys
     * @param valueAppliers  control value appliers
     */
    public SubmissionDecorationHook(
            TargetSelector targetSelector,
            FieldKeyResolver keyResolver,
            List<ControlValueApplier> valueAppliers
    ) {
        this.keyResolver = keyResolver;
        this.targetSelector = targetSelector;
        this.valueAppliers = List.copyOf(valueAppliers);
    }

    /**
     * Returns hook execution order.
     */
    @Override
    public int order() {
        return 1000;
    }

    /**
     * Applies submitted values and validation errors to form controls.
     */
    @Override
    public void afterMaterialize(Node root, RenderingExecution execution) {
        SubmissionState submission = readSubmission(execution.request().attributes());

        if (submission == null) {
            return;
        }

        Visitor<String> keys    = new Visitor.Default<>();
        Visitor<Node>   targets = new Visitor.Default<>(Collections.newSetFromMap(new IdentityHashMap<>()));

        root.execute(node -> {
            if (!isControlNode(node)) {
                return;
            }
            applyValue(node, submission);
            applyError(node, submission, targets, keys);
        });
    }

    /**
     * Applies submitted value to the given control node.
     */
    private void applyValue(Node node, SubmissionState submission) {
        String valueKey = keyResolver.resolveValueKey(node);

        if (valueKey == null || valueKey.isBlank() || !submission.hasValue(valueKey)) {
            return;
        }

        Object value = submission.getValue(valueKey);

        for (ControlValueApplier applier : valueAppliers) {
            if (applier.supports(node)) {
                applier.apply(node, value);
                return;
            }
        }
    }

    /**
     * Applies validation error state and feedback message to the given control node.
     */
    private void applyError(
            Node node,
            SubmissionState submission,
            Visitor<Node> targets,
            Visitor<String> keys
    ) {
        String errorKey = keyResolver.resolveErrorKey(node);

        if (errorKey == null || errorKey.isBlank() || !submission.hasError(errorKey)) {
            return;
        }

        Node target = targetSelector.resolve(node);

        if (target == null) {
            target = node;
        }

        target.addClass("is-invalid");

        if (targets.familiar(target) || keys.familiar(errorKey)) {
            return;
        }

        targets.visit(target);
        keys.visit(errorKey);

        Node feedback = invalidFeedback(submission.errorMessage(errorKey));

        target.insertAfter(feedback);
    }

    /**
     * Returns whether the node is a supported form control.
     */
    private boolean isControlNode(Node node) {
        if (node.getNodeType() != NodeType.ELEMENT) {
            return false;
        }

        TagName tag = node.getTagName();

        return tag == TagName.INPUT || tag == TagName.TEXTAREA || tag == TagName.SELECT;
    }

    /**
     * Reads submission state from request attributes.
     */
    private SubmissionState readSubmission(Map<String, Object> attributes) {
        return attributes.get(REQUEST_ATTRIBUTE) instanceof SubmissionState submission ? submission : null;
    }

    /**
     * Creates an invalid feedback node for the given message.
     */
    private Node invalidFeedback(String message) {
        Node div = new ElementNode(TagName.DIV);
        div.addAttribute("class", "invalid-feedback");
        div.append(new TextNode(message == null ? "" : message));
        return div;
    }
}