package org.jmouse.core.access.descriptor.internal;

import org.jmouse.core.access.descriptor.ConstructorDescriptor;
import org.jmouse.core.access.descriptor.FieldDescriptor;
import org.jmouse.core.access.descriptor.MethodDescriptor;
import org.jmouse.core.reflection.InferredType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClassTypeData extends AnnotatedElementData<Class<?>> {

    private final List<ConstructorDescriptor>   constructors = new ArrayList<>();
    private final Map<String, FieldDescriptor>  fields       = new HashMap<>();
    private final Map<String, MethodDescriptor> methods      = new HashMap<>();
    private       InferredType                  type;

    public ClassTypeData(Class<?> target) {
        super(target);
    }

    public InferredType getType() {
        return type;
    }

    public void setType(InferredType type) {
        this.type = type;
    }

    public List<ConstructorDescriptor> getConstructors() {
        return constructors;
    }

    public Map<String, FieldDescriptor> getFields() {
        return fields;
    }

    public Map<String, MethodDescriptor> getMethods() {
        return methods;
    }

    public void addConstructor(ConstructorDescriptor constructor) {
        constructors.add(constructor);
    }

    public void addField(FieldDescriptor field) {
        fields.put(field.getName(), field);
    }

    public void addMethod(MethodDescriptor method) {
        methods.put(method.unwrap().getName(), method);
    }

    public void clearConstructors() {
        constructors.clear();
    }

    public void clearFields() {
        fields.clear();
    }

    public void clearMethods() {
        methods.clear();
    }

}
