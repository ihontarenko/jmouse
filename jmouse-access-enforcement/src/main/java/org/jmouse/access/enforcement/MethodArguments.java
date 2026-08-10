package org.jmouse.access.enforcement;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * What a handler method's parameters are called, so a declaration can name one.
 *
 * <p>Resolved once per method rather than per call: a method's parameter names do not change while
 * the application runs, and reading annotations on the security path of every request is the sort of
 * cost that never shows up as anything but a slow product.
 *
 * <p>Which name counts is {@link ParameterNaming}'s question, not this class's — see there for why it
 * has to be asked rather than assumed.
 */
public final class MethodArguments {

    private final List<Argument> arguments;

    private MethodArguments(List<Argument> arguments) {
        this.arguments = arguments;
    }

    public static MethodArguments of(Method method, ParameterNaming naming) {
        Parameter[]    parameters = method.getParameters();
        List<Argument> arguments  = new ArrayList<>(parameters.length);

        for (int position = 0; position < parameters.length; position++) {
            Parameter parameter = parameters[position];

            arguments.add(new Argument(
                    position,
                    naming.requestName(parameter).orElseGet(parameter::getName),
                    naming.isIdentifier(parameter)));
        }

        return new MethodArguments(List.copyOf(arguments));
    }

    /** The value passed for the argument with this name, where it is a non-blank string. */
    public Optional<String> valueOf(String name, Object[] values) {
        return arguments.stream()
                .filter(argument -> argument.name().equals(name))
                .findFirst()
                .flatMap(argument -> textAt(argument.position(), values));
    }

    /**
     * The one identifier this method takes, where it takes exactly one.
     *
     * <p>What {@code resourceId} defaults to, and what almost every by-identifier route has. Where
     * there are two, the declaration validator refuses to start rather than picking the first —
     * guessing which identifier a route is about is how an authorization check comes to be made about
     * the wrong row.
     */
    public Optional<String> soleIdentifierName() {
        List<Argument> identifiers = arguments.stream().filter(Argument::identifier).toList();

        return identifiers.size() == 1
                ? Optional.of(identifiers.getFirst().name())
                : Optional.empty();
    }

    public boolean names(String name) {
        return arguments.stream().anyMatch(argument -> argument.name().equals(name));
    }

    private Optional<String> textAt(int position, Object[] values) {
        if (values == null || position >= values.length || !(values[position] instanceof String text)) {
            return Optional.empty();
        }

        return text.isBlank() ? Optional.empty() : Optional.of(text);
    }

    private record Argument(int position, String name, boolean identifier) {}
}
