package org.jmouse.template;

public record DirectiveOutcome<T>(
        boolean omitted,
        T node,
        T root
) {

    public static <T> DirectiveOutcome<T> keep(T node) {
        return new DirectiveOutcome<>(false, node, node);
    }

    public static <T> DirectiveOutcome<T> omit() {
        return new DirectiveOutcome<>(true, null, null);
    }

    public static <T> DirectiveOutcome<T> wrapped(T node, T root) {
        return new DirectiveOutcome<>(false, node, root);
    }

    public boolean isOmitted() {
        return omitted;
    }

}
