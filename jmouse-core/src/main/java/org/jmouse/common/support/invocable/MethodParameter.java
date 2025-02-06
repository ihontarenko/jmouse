package org.jmouse.common.support.invocable;

public final class MethodParameter {

    private final int                 index;
    private final Object              value;
    private final ClassTypeDescriptor descriptor;

    public MethodParameter(int index, Object value) {
        this.index = index;
        this.value = value;
        this.descriptor = new ReflectionClassTypeDescriptor(value);
    }

    public int getIndex() {
        return index;
    }

    public Object getValue() {
        return value;
    }

    public ClassTypeDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public String toString() {
        return "[%d] %s".formatted(index, descriptor.getName());
    }

}

