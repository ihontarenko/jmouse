package svit.context;

import svit.reflection.Reflections;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

abstract public class AbstractAttributesContext implements AttributesContext {

    private final Map<Object, Object> attributes = new HashMap<>();

    @Override
    public <T> T requireAttribute(Object name) {
        T value = getAttribute(name);

        if (value == null) {
            throw new AttributeNotFoundException("Required attribute not found [%s]".formatted(name));
        }

        return value;
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public <T> T getAttribute(Object name) {
        return (T) attributes.get(name);
    }

    @Override
    public void setAttribute(Object name, Object argument) {
        attributes.put(name, argument);
    }

    @Override
    public void setAttribute(Object argument) {
        setAttribute(Reflections.getUserClass(requireNonNull(argument).getClass()), argument);
    }

    @Override
    public void setAttributes(Object... attributes) {
        for (Object argument : attributes) {
            setAttribute(argument);
        }
    }

    @Override
    public boolean hasAttribute(Object name) {
        return attributes.containsKey(name);
    }

    @Override
    public String toString() {
        return "ARGUMENTS: %s".formatted(attributes);
    }

}
