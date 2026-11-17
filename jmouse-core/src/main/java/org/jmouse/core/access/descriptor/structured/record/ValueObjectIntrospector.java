package org.jmouse.core.access.descriptor.structured.record;

import org.jmouse.core.access.descriptor.*;
import org.jmouse.core.access.descriptor.structured.PropertyDescriptor;
import org.jmouse.core.reflection.InferredType;
import org.jmouse.core.Streamable;

import java.lang.reflect.RecordComponent;
import java.util.*;

public class ValueObjectIntrospector<T>
        extends AbstractIntrospector<ValueObjectData<T>, ValueObjectIntrospector<T>, T, ValueObjectDescriptor<T>> {

    public ValueObjectIntrospector(Class<T> target) {
        super(null);
        type(target);
    }

    public ValueObjectIntrospector<T> type(Class<T> type) {
        ClassTypeIntrospector introspector = new ClassTypeIntrospector(InferredType.forClass(type));
        container.setType(introspector.introspect().toDescriptor());
        return self();
    }

    public ValueObjectIntrospector<T> constructor() {
        ClassTypeDescriptor         descriptor   = container.getType();
        List<ConstructorDescriptor> constructors = new ArrayList<>(descriptor.getConstructors());

        // ⚠️ A record is made by its canonical constructor: a component for each parameter. It took the constructor with the
        // fewest parameters, and a record with a constructor of convenience — fewer arguments, the rest defaulted — was then
        // handed every component of it and refused ("wrong number of arguments"). Any other value object keeps that choice.
        RecordComponent[] components = descriptor.getClassType().isRecord() ? descriptor.getClassType().getRecordComponents() : null;

        if (components != null) {
            for (ConstructorDescriptor constructor : constructors) {
                if (isCanonical(new ArrayList<>(constructor.getParameters()), components)) {
                    container.setConstructor(constructor);
                    return self();
                }
            }
        }

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

        RecordComponent[]         ordered    = parent.isValueObject() ? classType.getRecordComponents() : new RecordComponent[0];
        List<ParameterDescriptor> parameters = new ArrayList<>(constructor.getParameters());
        boolean                   canonical  = isCanonical(parameters, ordered);

        if (parent.isValueObject()) {
            components = Streamable.of(ordered).toMap(RecordComponent::getName);
        }

        for (int index = 0; index < parameters.size(); index++) {
            ParameterDescriptor parameter = parameters.get(index);

            // ⚠️ The canonical constructor's parameters ARE the components, in their order — the language makes it so — while
            // their names are kept only in a MethodParameters attribute: javac writes one for every canonical constructor, but a
            // compiler that keeps none — TeaVM, compiling a record for the browser — or a tool that strips it leaves arg0, arg1,
            // and read by name every property was missing. So a component is found by its position, and by name only for a
            // constructor that is not the canonical one.
            RecordComponent component = canonical ? ordered[index] : components.get(parameter.getName());

            ValueObjectPropertyIntrospector<T> introspector = new ValueObjectPropertyIntrospector<>(
                    container.getTarget());
            introspector.owner(parent).name(component != null ? component.getName() : parameter.getName());

            // ⚠️ The component, not the parameter, when there is one. A record whose canonical
            // constructor is compact reflects its parameters WITHOUT generics —
            // Parameter.getParameterizedType() answers a bare `Set` where the source says
            // `Set<String>` — while the record component keeps the full type. Reading the parameter
            // therefore lost every element type, and a collection property bound through
            // CollectionBinder with nothing to bind its elements as: "No binder found for bindable
            // type 'Object'", raised only once the property actually carried values, which is why an
            // empty list always looked fine.
            introspector.type(component == null
                                      ? parameter.getType()
                                      : new ClassTypeIntrospector(
                                              InferredType.forType(component.getGenericType()))
                                              .introspect().toDescriptor());

            if (component != null) {
                MethodDescriptor method = new MethodIntrospector(component.getAccessor()).introspect()
                        .toDescriptor();
                introspector.getterMethod(method);
            }

            ValueObjectPropertyDescriptor<T> propertyDescriptor = introspector.toDescriptor();

            property(propertyDescriptor);
            component(propertyDescriptor);
        }

        ClassTypeDescriptor descriptor = container.getType();

        for (Map.Entry<String, MethodDescriptor> entry : descriptor.getMethods().entrySet()) {
            MethodDescriptor      method   = entry.getValue();
            PropertyDescriptor<T> previous = container.getProperty(method.getName());

            if (previous instanceof ValueObjectPropertyDescriptor<?> ignored) {
                continue;
            }

            AnnotationDescriptor annotationDescriptor = method.findAnnotation(DescriptorProperty.class);

            if (annotationDescriptor != null) {
                ValueObjectDescriptor<T>           parentDescriptor     = toDescriptor();
                ValueObjectPropertyIntrospector<T> propertyIntrospector = new ValueObjectPropertyIntrospector<>(null);
                propertyIntrospector.owner(parentDescriptor).name(
                        String.valueOf(annotationDescriptor.getAttribute("value"))
                ).type(method.getReturnType()).getterMethod(method);
                container.addProperty(propertyIntrospector.toDescriptor());
            }
        }

        return self();
    }

    /**
     * Whether the constructor is the record's canonical one: a parameter for each component, of its type, in its order.
     *
     * @param parameters the constructor's parameters, in order
     * @param components the record's components, in order; none for a value object that is no record
     * @return {@code true} when each parameter stands for the component at its position
     */
    private static boolean isCanonical(List<ParameterDescriptor> parameters, RecordComponent[] components) {
        if (components.length == 0 || parameters.size() != components.length) {
            return false;
        }

        for (int index = 0; index < components.length; index++) {
            if (parameters.get(index).unwrap().getType() != components[index].getType()) {
                return false;
            }
        }

        return true;
    }

    public ValueObjectIntrospector<T> property(ValueObjectPropertyDescriptor<T> property) {
        container.addProperty(property);
        return self();
    }

    public ValueObjectIntrospector<T> component(ValueObjectPropertyDescriptor<T> property) {
        container.addComponent(property);
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
