package org.jmouse.el.type;

import org.jmouse.el.AbstractObjectContainer;

/** The types a language knows, by the name a document writes. */
public class TypeContainer extends AbstractObjectContainer<String, ValueType> {

    @Override
    public String keyFor(ValueType type) {
        return type.name();
    }

    @Override
    public String toString() {
        return "TYPES: " + super.toString();
    }
}
