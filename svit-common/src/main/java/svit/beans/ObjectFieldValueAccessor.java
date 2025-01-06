package svit.beans;

import java.lang.reflect.Field;

public class ObjectFieldValueAccessor implements FieldAccessor {

    private final Object object;
    private final Field  field;

    public ObjectFieldValueAccessor(Object object, Field field) {
        this.object = object;
        this.field = field;
    }

    @Override
    public Object getValue() {
        try {
            return field.get(object);
        } catch (IllegalAccessException e) {
            throw new BeanFieldNotFoundException("UNABLE TO ACCESS FIELD VALUE", e);
        }
    }

    @Override
    public void setValue(Object value) {
        try {
            field.set(object, value);
        } catch (IllegalAccessException e) {
            throw new BeanFieldNotFoundException("UNABLE TO SET FIELD VALUE", e);
        }
    }
}
