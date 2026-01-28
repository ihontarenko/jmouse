package org.jmouse.core.bind.descriptor.structured;

import org.jmouse.core.bind.descriptor.AnnotatedElementDescriptor;
import org.jmouse.core.bind.descriptor.PreferredPropertyName;
import org.jmouse.core.reflection.Reflections;

final public class ObjectIntrospector {

    public static String getPreferredPropertyName(AnnotatedElementDescriptor<?, ?, ?> descriptor) {
        return Reflections.getAnnotationValue(
                descriptor.unwrap(),
                PreferredPropertyName.class,
                PreferredPropertyName::value
        );
    }

}
