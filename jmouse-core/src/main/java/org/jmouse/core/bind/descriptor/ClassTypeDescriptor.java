package org.jmouse.core.bind.descriptor;

import org.jmouse.core.bind.descriptor.internal.ClassTypeData;
import org.jmouse.core.reflection.TypeClassifier;
import org.jmouse.core.reflection.InferredType;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ClassTypeDescriptor extends AnnotatedElementDescriptor<Class<?>, ClassTypeData, ClassTypeIntrospector> implements TypeClassifier {

    public ClassTypeDescriptor(ClassTypeIntrospector introspector, ClassTypeData container) {
        super(introspector, container);
    }

    public InferredType getJavaType() {
        return container.getType();
    }

    @Override
    public Class<?> getClassType() {
        return getJavaType().getClassType();
    }

    public List<ConstructorDescriptor> getConstructors() {
        return Collections.unmodifiableList(container.getConstructors());
    }

    public ConstructorDescriptor getConstructor(int index) {
        return container.getConstructors().get(index);
    }

    public Map<String, MethodDescriptor> getMethods() {
        return Collections.unmodifiableMap(container.getMethods());
    }

    public MethodDescriptor getMethod(String name) {
        return getMethods().get(name);
    }

    public boolean hasMethod(String methodName) {
        return getMethods().containsKey(methodName);
    }

    public Map<String, FieldDescriptor> getFields() {
        return Collections.unmodifiableMap(container.getFields());
    }

    public FieldDescriptor getField(String name) {
        return getFields().get(name);
    }

    public boolean hasField(String name) {
        return getFields().containsKey(name);
    }

    @Override
    public ClassTypeIntrospector toIntrospector() {
        return introspector;
    }

    @Override
    public String toString() {
        return getJavaType().toString();
    }

}
