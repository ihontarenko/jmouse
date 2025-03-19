package org.jmouse.core.bind.descriptor;

import org.jmouse.core.bind.descriptor.internal.DataContainer;

public interface Descriptor<T, C extends DataContainer<T>, I extends Introspector<?, ?, ?, ?>> {

    String getName();

    T unwrap();

    I toIntrospector();

}
