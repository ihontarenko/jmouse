package svit.support.objects;

import java.beans.PropertyDescriptor;
import java.util.Collections;
import java.util.Map;

public class NullObjectBeanInfo implements BeanObjectInfo {

    @Override
    public <T> T getObject() {
        return (T) new Object();
    }

    @Override
    public Class<?> getClassType() {
        return void.class;
    }

    @Override
    public Map<String, BeanField> getBeanFields() {
        return Collections.emptyMap();
    }

    @Override
    public BeanField getBeanField(String fieldName) {
        return new NullBeanField();
    }

    @Override
    public PropertyDescriptor getPropertyDescriptor(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, PropertyDescriptor> getPropertyDescriptors() {
        throw new UnsupportedOperationException();
    }
}

