package org.jmouse.dom.blueprint.model;

/**
 * Control semantics describe how a control should behave and how it is expected to be rendered.
 *
 * <p>This separates a "tag" from intent. For example:</p>
 * <ul>
 *   <li>{@code TagName.INPUT} + {@code kind=INPUT} + {@code inputType=text}</li>
 *   <li>{@code TagName.SELECT} + {@code kind=SELECT}</li>
 *   <li>{@code TagName.INPUT} + {@code kind=RADIO_GROUP} (expanded rendering)</li>
 * </ul>
 */
public record ControlSemantics(
        ControlKind kind,
        String inputType
) {

    public ControlSemantics {
        kind = kind == null ? ControlKind.NONE : kind;
    }

    public static ControlSemantics none() {
        return new ControlSemantics(ControlKind.NONE, null);
    }

    public static ControlSemantics input(String inputType) {
        return new ControlSemantics(ControlKind.INPUT, inputType);
    }

    public static ControlSemantics select() {
        return new ControlSemantics(ControlKind.SELECT, null);
    }

    public static ControlSemantics textarea() {
        return new ControlSemantics(ControlKind.TEXTAREA, null);
    }

    public static ControlSemantics button() {
        return new ControlSemantics(ControlKind.BUTTON, null);
    }

    public static ControlSemantics radioGroup() {
        return new ControlSemantics(ControlKind.RADIO_GROUP, "radio");
    }

    public static ControlSemantics checkboxGroup() {
        return new ControlSemantics(ControlKind.CHECKBOX_GROUP, "checkbox");
    }
}
