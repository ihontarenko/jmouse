package org.jmouse.common.support.context;

import java.util.HashMap;
import java.util.Map;

abstract public class AbstractArgumentsContext implements ArgumentsContext {

    private final Map<Object, Object> arguments = new HashMap<>();

    @Override
    @SuppressWarnings({"unchecked"})
    public <T> T getArgument(Object name) {
        return (T) arguments.get(name);
    }

    @Override
    public void setArgument(Object name, Object argument) {
        arguments.put(name, argument);
    }

    @Override
    public boolean hasArgument(Object name) {
        return arguments.containsKey(name);
    }

    @Override
    public String toString() {
        return "ARGUMENTS: %s".formatted(arguments);
    }

}
