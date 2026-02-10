package org.jmouse.core.access.descriptor;

import org.jmouse.core.access.descriptor.internal.DataContainer;

public interface Descriptor<T, C extends DataContainer<T>, I extends Introspector<?, ?, ?, ?>> {

    String getName();

    T unwrap();

    I toIntrospector();

}
