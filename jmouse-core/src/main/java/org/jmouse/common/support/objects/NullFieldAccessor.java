package org.jmouse.common.support.objects;

public class NullFieldAccessor implements FieldAccessor {

    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public void setValue(Object value) {
        throw new UnsupportedOperationException();
    }

}
