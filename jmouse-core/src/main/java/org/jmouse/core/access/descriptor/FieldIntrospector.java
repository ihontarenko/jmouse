package org.jmouse.core.access.descriptor;

import org.jmouse.core.access.descriptor.internal.FieldData;
import org.jmouse.core.reflection.InferredType;
import org.jmouse.core.reflection.Reflections;

import java.lang.reflect.Field;

/**
 * Performs descriptor of Java class fields, capturing metadata such as name, annotations, and type information.
 *
 * <p>Typical usage:</p>
 * <pre>{@code
 * Field field = MyClass.class.getDeclaredField("myField");
 * FieldDescriptor descriptor = new FieldIntrospector(field).introspect().toDescriptor();
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class FieldIntrospector extends AnnotatedElementIntrospector<FieldData, FieldIntrospector, Field, FieldDescriptor> {

    protected FieldIntrospector(Field target) {
        super(target);
    }

    /**
     * Sets the name of the introspected field.
     *
     * @return current introspector instance
     */
    @Override
    public FieldIntrospector name() {
        return name(Reflections.getFieldName(container.getTarget()));
    }

    /**
     * Performs a complete descriptor including annotations, name, and type information.
     *
     * @return current introspector instance
     */
    @Override
    public FieldIntrospector introspect() {
        return annotations().name().type();
    }

    /**
     * Introspects and sets detailed type information of the field.
     *
     * @return current introspector instance
     */
    public FieldIntrospector type() {
        ClassTypeIntrospector introspector = new ClassTypeIntrospector(InferredType.forField(container.getTarget()));
        container.setType(introspector.annotations().name().toDescriptor());
        return self();
    }

    /**
     * Converts introspected metadata into a {@link FieldDescriptor}.
     *
     * @return descriptor containing introspected field data
     */
    @Override
    public FieldDescriptor toDescriptor() {
        return getCachedDescriptor(() -> new FieldDescriptor(this, container));
    }

    /**
     * Creates a container for holding introspected data related to the specified field.
     *
     * @param target the field to introspect
     * @return container for field metadata
     */
    @Override
    public FieldData getContainerFor(Field target) {
        return new FieldData(target);
    }
}
