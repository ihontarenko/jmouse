package org.jmouse.dom.blueprint.hooks;

import org.jmouse.dom.Node;
import org.jmouse.dom.NodeType;
import org.jmouse.dom.TagName;
import org.jmouse.dom.blueprint.RenderingExecution;
import org.jmouse.dom.blueprint.SubmissionState;
import org.jmouse.dom.node.ElementNode;
import org.jmouse.dom.node.TextNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Applies submitted values and validation errors to the rendered node tree.
 *
 * <p>This hook keeps blueprints independent from submission mechanics.</p>
 *
 * <p>Rules:</p>
 * <ul>
 *   <li>Finds control elements by attribute {@code name}.</li>
 *   <li>Applies submitted value:
 *     <ul>
 *       <li>{@code input}: attribute {@code value}</li>
 *       <li>{@code textarea}: sets text child</li>
 *       <li>{@code select}: sets {@code selected} on matching {@code option}</li>
 *     </ul>
 *   </li>
 *   <li>If field has error:
 *     <ul>
 *       <li>adds {@code is-invalid} to class</li>
 *       <li>inserts {@code <div class="invalid-feedback">message</div>} after the control element</li>
 *     </ul>
 *   </li>
 * </ul>
 */
public final class SubmissionDecorationHook implements RenderingHook {

    @Override
    public int order() {
        return 1000;
    }

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

    private SubmissionState readSubmission(Map<String, Object> attributes) {
        Object value = attributes.get(SubmissionState.REQUEST_ATTRIBUTE);
        if (value instanceof SubmissionState submissionState) {
            return submissionState;
        }
        return null;
    }

    private boolean isControlTag(TagName tagName) {
        return tagName == TagName.INPUT || tagName == TagName.TEXTAREA || tagName == TagName.SELECT;
    }

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
            replaceChildrenWithText(node, text);
            return;
        }

        if (tagName == TagName.SELECT) {
            selectOption(node, text);
        }
    }

    private void applyError(Node node, String fieldName, SubmissionState submission) {
        if (!submission.hasError(fieldName)) {
            return;
        }

        String message = submission.errorMessage(fieldName);
        addClass(node, "is-invalid");

        Node feedback = invalidFeedback(message);

        node.insertAfter(feedback);
    }

    private void addClass(Node node, String className) {
        String existing = node.getAttribute("class");

        if (existing == null || existing.isBlank()) {
            node.addAttribute("class", className);
            return;
        }

        if (containsClass(existing, className)) {
            return;
        }

        node.addAttribute("class", existing + " " + className);
    }

    private boolean containsClass(String classValue, String required) {
        String[] parts = classValue.trim().split("\\s+");
        for (String string : parts) {
            if (string.equals(required)) {
                return true;
            }
        }
        return false;
    }

    private Node invalidFeedback(String message) {
        ElementNode div = new ElementNode(TagName.DIV);
        div.addAttribute("class", "invalid-feedback");
        div.append(new TextNode(message == null ? "" : message));
        return div;
    }

    private void replaceChildrenWithText(Node node, String text) {
        List<Node> childrenCopy = new ArrayList<>(node.getChildren());
        for (Node child : childrenCopy) {
            node.removeChild(child);
        }
        node.append(new TextNode(text));
    }

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
