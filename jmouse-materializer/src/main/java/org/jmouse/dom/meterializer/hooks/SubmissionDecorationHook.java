package org.jmouse.dom.meterializer.hooks;

import org.jmouse.dom.Node;
import org.jmouse.dom.NodeType;
import org.jmouse.dom.TagName;
import org.jmouse.meterializer.RenderingExecution;
import org.jmouse.meterializer.SubmissionState;
import org.jmouse.dom.node.ElementNode;
import org.jmouse.dom.node.TextNode;
import org.jmouse.meterializer.hooks.RenderingHook;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Applies submitted values and validation errors to the materialized DOM tree. 🧾✅
 *
 * <p>
 * This hook runs <b>after</b> materialization and decorates the produced node tree
 * based on {@link SubmissionState} stored in request attributes. The goal is to keep
 * blueprints independent from submission mechanics (PRG / validation / field errors).
 * </p>
 *
 * <h3>Contract</h3>
 * <ul>
 *   <li>Controls are detected by {@code name} attribute.</li>
 *   <li>Supported control tags: {@code input}, {@code textarea}, {@code select}.</li>
 * </ul>
 *
 * <h3>Value application rules</h3>
 * <ul>
 *   <li>{@code input}: sets attribute {@code value}</li>
 *   <li>{@code textarea}: replaces children with a single {@link TextNode}</li>
 *   <li>{@code select}: marks matching {@code option} by setting {@code selected="selected"}</li>
 * </ul>
 *
 * <h3>Error decoration rules</h3>
 * <ul>
 *   <li>Adds {@code is-invalid} to the control's CSS class list</li>
 *   <li>Inserts feedback node after the control:
 *     {@code <div class="invalid-feedback">message</div>}</li>
 * </ul>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * RenderingRequest request = new RenderingRequest();
 * request.attributes().put(SubmissionState.REQUEST_ATTRIBUTE, submissionState);
 *
 * Node node = pipeline.render("form", model, r -> request);
 * // After materialize: inputs are filled, invalid controls have feedback nodes.
 * }</pre>
 *
 * <p>
 * ⚠ This hook mutates the DOM tree (adds attributes, replaces children, inserts siblings).
 * Make sure it is executed after all structural operations that rely on original children.
 * </p>
 */
public final class SubmissionDecorationHook implements RenderingHook<Node> {

    /**
     * Runs after most hooks to apply final DOM decorations.
     *
     * @return hook order
     */
    @Override
    public int order() {
        return 1000;
    }

    /**
     * Decorates the materialized DOM tree based on {@link SubmissionState}.
     *
     * @param root materialized root node
     * @param execution rendering execution context
     */
    @Override
    public void afterMaterialize(Node root, RenderingExecution execution) {
        SubmissionState submission = readSubmission(execution.request().attributes());
        if (submission == null) {
            return;
        }

        root.execute(node -> {
            if (node.getNodeType() != NodeType.ELEMENT) {
                return;
            }

            TagName tagName = node.getTagName();
            if (!isControlTag(tagName)) {
                return;
            }

            String fieldName = node.getAttribute("name");
            if (fieldName == null || fieldName.isBlank()) {
                return;
            }

            applyValue(node, tagName, fieldName, submission);
            applyError(node, fieldName, submission);
        });
    }

    /**
     * Reads {@link SubmissionState} from request attributes.
     *
     * @param attributes request attributes
     * @return submission state or {@code null} if absent
     */
    private SubmissionState readSubmission(Map<String, Object> attributes) {
        Object value = attributes.get(SubmissionState.REQUEST_ATTRIBUTE);
        if (value instanceof SubmissionState submissionState) {
            return submissionState;
        }
        return null;
    }

    /**
     * Checks whether given tag name is treated as a form control.
     *
     * @param tagName tag name
     * @return {@code true} for {@code input}, {@code textarea}, {@code select}
     */
    private boolean isControlTag(TagName tagName) {
        return tagName == TagName.INPUT || tagName == TagName.TEXTAREA || tagName == TagName.SELECT;
    }

    /**
     * Applies submitted value to a control node.
     *
     * @param node control node
     * @param tagName control tag name
     * @param fieldName field name (value lookup key)
     * @param submission submission state
     */
    private void applyValue(Node node, TagName tagName, String fieldName, SubmissionState submission) {
        if (!submission.hasValue(fieldName)) {
            return;
        }

        Object value = submission.value(fieldName);
        String text  = value == null ? "" : String.valueOf(value);

        if (tagName == TagName.INPUT) {
            node.addAttribute("value", text);
            return;
        }

        if (tagName == TagName.TEXTAREA) {
            pubTextNode(node, text);
            return;
        }

        if (tagName == TagName.SELECT) {
            selectOption(node, text);
        }
    }

    /**
     * Applies validation error decoration if present.
     *
     * @param node control node
     * @param fieldName field name (error lookup key)
     * @param submission submission state
     */
    private void applyError(Node node, String fieldName, SubmissionState submission) {
        if (!submission.hasError(fieldName)) {
            return;
        }

        String message = submission.errorMessage(fieldName);

        node.addClass("is-invalid");
        Node feedback = invalidFeedback(message);
        node.insertAfter(feedback);
    }

    /**
     * Builds {@code <div class="invalid-feedback">message</div>} node.
     *
     * @param message error message
     * @return feedback node
     */
    private Node invalidFeedback(String message) {
        ElementNode div = new ElementNode(TagName.DIV);
        div.addAttribute("class", "invalid-feedback");
        div.append(new TextNode(message == null ? "" : message));
        return div;
    }

    /**
     * Replaces all children of the node with a single text node.
     *
     * @param node node to modify
     * @param text new text content
     */
    private void pubTextNode(Node node, String text) {
        List<Node> childrenCopy = new ArrayList<>(node.getChildren());
        for (Node child : childrenCopy) {
            node.removeChild(child);
        }
        node.append(new TextNode(text));
    }

    /**
     * Marks an {@code <option>} child as selected by comparing its {@code value} attribute.
     *
     * @param selectNode select element node
     * @param selectedValue selected value
     */
    private void selectOption(Node selectNode, String selectedValue) {
        for (Node child : selectNode.getChildren()) {
            if (child.getNodeType() != NodeType.ELEMENT) {
                continue;
            }
            if (child.getTagName() != TagName.OPTION) {
                continue;
            }

            String optionValue = child.getAttribute("value");

            if (optionValue != null && optionValue.equals(selectedValue)) {
                child.addAttribute("selected", "selected");
            }
        }
    }
}