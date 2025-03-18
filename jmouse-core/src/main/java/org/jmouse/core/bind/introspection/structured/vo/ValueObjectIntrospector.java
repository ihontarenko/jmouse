package org.jmouse.core.bind.introspection.structured.vo;

import org.jmouse.core.bind.introspection.*;
import org.jmouse.core.reflection.JavaType;
import org.jmouse.util.Streamable;

import java.lang.reflect.RecordComponent;
import java.util.*;

public class ValueObjectIntrospector<T>
        extends AbstractIntrospector<ValueObjectData<T>, ValueObjectIntrospector<T>, T, ValueObjectDescriptor<T>> {

    public ValueObjectIntrospector(Class<T> target) {
        super(null);
        type(target);
    }

    public ValueObjectIntrospector<T> type(Class<T> type) {
        ClassTypeIntrospector introspector = new ClassTypeIntrospector(JavaType.forClass(type));
        container.setType(introspector.introspect().toDescriptor());
        return self();
    }

    public ValueObjectIntrospector<T> constructor() {
        ClassTypeDescriptor         descriptor   = container.getType();
        List<ConstructorDescriptor> constructors = new ArrayList<>(descriptor.getConstructors());

        constructors.sort(Comparator.comparingInt(ctor -> ctor.getParameters().size()));
        container.setConstructor(constructors.getFirst());

        return self();
    }

    public ValueObjectIntrospector<T> properties() {
        constructor();

        Map<String, RecordComponent> components  = Collections.emptyMap();
        ConstructorDescriptor        constructor = container.getConstructor();
        ValueObjectDescriptor<T>     parent      = toDescriptor();
        Class<?>                     classType   = parent.getClassType();

        if (parent.isValueObject()) {
            components = Streamable.of(classType.getRecordComponents()).toMap(RecordComponent::getName);
        }

        for (ParameterDescriptor parameter : constructor.getParameters()) {
            ValueObjectPropertyIntrospector<T> introspector = new ValueObjectPropertyIntrospector<>(
                    container.getTarget());
            introspector.owner(parent).name(parameter.getName());

            introspector.type(parameter.getType());

            if (components.containsKey(parameter.getName())) {
                RecordComponent  component = components.get(parameter.getName());
                MethodDescriptor method    = new MethodIntrospector(component.getAccessor()).introspect()
                        .toDescriptor();
                introspector.getterMethod(method);
            }

            property(introspector.toDescriptor());
        }

        return self();
    }

    public ValueObjectIntrospector<T> property(ValueObjectPropertyDescriptor<T> property) {
        container.addProperty(property);
        return self();
    }

    @Override
    public ValueObjectIntrospector<T> name() {
        return name(container.getType().getName());
    }

    @Override
    public ValueObjectIntrospector<T> introspect() {
        return name().properties();
    }

    @Override
    public ValueObjectDescriptor<T> toDescriptor() {
        return getCachedDescriptor(() -> new ValueObjectDescriptor<>(this, container));
    }

    @Override
    public ValueObjectData<T> getContainerFor(T target) {
        return new ValueObjectData<>(target);
    }
}
