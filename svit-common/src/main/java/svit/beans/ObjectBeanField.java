package svit.beans;

import java.lang.reflect.Field;

public class ObjectBeanField implements BeanField {

    private final Field  field;
    private final Object object;

    public ObjectBeanField(Field field, Object object) {
        this.field = field;
        this.object = object;
    }

    @Override
    public String getName() {
        return field.getName();
    }

    @Override
    public FieldAccessor getFieldAccessor() {
        return new ObjectFieldValueAccessor(object, field);
    }

}
