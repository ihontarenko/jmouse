package org.jmouse.core.mapping;

import org.jmouse.core.access.PropertyPath;
import org.jmouse.core.reflection.InferredType;

public sealed interface MappingDestination
        permits MappingDestination.RootTarget,
                MappingDestination.BeanProperty,
                MappingDestination.RecordComponent,
                MappingDestination.MapEntry,
                MappingDestination.CollectionElement {

    Object target();

    PropertyPath path();

    record RootTarget(Object target, PropertyPath path) implements MappingDestination {
    }

    record BeanProperty(Object target, PropertyPath path, Object propertyDescriptor) implements MappingDestination {
    }

    record RecordComponent(Object target, PropertyPath path, Object componentDescriptor) implements MappingDestination {
    }

    record MapEntry(Object target, PropertyPath path, Object key, InferredType valueType) implements MappingDestination {
    }

    record CollectionElement(Object target, PropertyPath path, int index, InferredType elementType)
            implements MappingDestination {
    }
}

