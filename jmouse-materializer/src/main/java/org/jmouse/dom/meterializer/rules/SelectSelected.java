package org.jmouse.dom.meterializer.rules;

import org.jmouse.dom.Node;
import org.jmouse.dom.TagName;
import org.jmouse.dom.node.ElementNode;
import org.jmouse.dom.meterializer.NodeRule;
import org.jmouse.meterializer.RenderingExecution;
import org.jmouse.meterializer.SubmissionState;

import java.util.Optional;

/**
 * {@link NodeRule} that marks the selected {@code <option>} in a {@code <select>}
 * based on submitted form values.
 *
 * <p>
 * Uses {@link SubmissionState} from the rendering request to determine
 * which option should be marked as {@code selected}.
 * </p>
 */
public final class SelectSelected implements NodeRule {

    /**
     * Execution order among other {@link NodeRule}s.
     */
    @Override
    public int order() {
        return 500;
    }

    /**
     * Matches {@code <select>} elements.
     */
    @Override
    public boolean matches(Node node, RenderingExecution execution) {
        return node.getTagName() == TagName.SELECT;
    }

    /**
     * Applies the selected state to matching {@code <option>} elements.
     *
     * <p>
     * The submitted value is compared with the {@code value} attribute
     * of each option.
     * </p>
     */
    @Override
    public void apply(Node node, RenderingExecution execution) {
        ElementNode select = (ElementNode) node;

        Optional<SubmissionState> submission =
                execution.request().findAttribute(SubmissionState.REQUEST_ATTRIBUTE, SubmissionState.class);

        SubmissionState submissionState = submission.orElseThrow();

        String name      = select.getAttribute("name");
        Object submitted = submissionState.value(name);
        String expected  = String.valueOf(submitted);

        for (Node child : select.getChildren()) {
            if (!(child instanceof ElementNode option) || option.getTagName() != TagName.OPTION) {
                continue;
            }

            String value = option.getAttribute("value");

            if (value != null && value.equals(expected)) {
                option.addAttribute("selected", "selected");
            } else {
                option.getAttributes().remove("selected");
            }
        }
    }
}