package svit.support.objects;

import org.jmouse.core.reflection.Reflections;

import java.beans.PropertyDescriptor;
import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

abstract public class AbstractBeanInfo implements BeanObjectInfo {

    protected final Class<?>                        classType;
    protected final Object                          object;
    protected final Map<String, PropertyDescriptor> descriptors = new HashMap<>();
    protected       Map<String, BeanField>          fields;

    public AbstractBeanInfo(Object object) {
        this.classType = Reflections.getAnonymousClass(requireNonNull(object).getClass());
        this.object = object;
        this.fields = new HashMap<>();
    }

    @Override
    public <T> T getObject() {
        return (T) object;
    }

    @Override
    public Class<?> getClassType() {
        return classType;
    }

    @Override
    public Map<String, BeanField> getBeanFields() {
        return fields;
    }

    @Override
    public PropertyDescriptor getPropertyDescriptor(String name) {
        throw new UnsupportedOperationException(
                "Property descriptors cannot be retrieved for class type: " + getClassType());
    }

    @Override
    public Map<String, PropertyDescriptor> getPropertyDescriptors() {
        return descriptors;
    }

    @Override
    public BeanField getBeanField(String name) {
        return ofNullable(fields.get(name)).orElseThrow(() -> new BeanFieldNotFoundException(
                "No fields are defined for the bean info of the class: " + getClassType()));
    }
}
