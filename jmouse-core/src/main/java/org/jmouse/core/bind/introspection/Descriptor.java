package org.jmouse.core.bind.introspection;

import org.jmouse.core.bind.introspection.internal.DataContainer;

public interface Descriptor<T, C extends DataContainer<T>, I extends Introspector<?, ?, ?, ?>> {

    String getName();

    T unwrap();

    I toIntrospector();

}
