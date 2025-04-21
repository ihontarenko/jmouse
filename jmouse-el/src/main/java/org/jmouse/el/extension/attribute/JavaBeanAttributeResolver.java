package org.jmouse.el.extension.attribute;

import org.jmouse.core.bind.descriptor.structured.jb.JavaBeanDescriptor;
import org.jmouse.core.bind.descriptor.structured.jb.JavaBeanIntrospector;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class JavaBeanAttributeResolver implements AttributeResolver {

    private static final Map<String, Method> CACHE = new HashMap<>();

    @Override
    public Object resolve(Object instance, String name) {
        Method getter = null;
        Object value  = null;

        if (instance != null) {
            Class<?> clazz = instance.getClass();
            String   key   = clazz.getName() + "." + name;

            getter = CACHE.get(key);

            if (getter == null) {
                JavaBeanDescriptor<?> descriptor = new JavaBeanIntrospector<>(clazz).introspect().toDescriptor();
                if (descriptor.hasProperty(name)) {
                    getter = descriptor.getProperty(name).getGetterMethod().unwrap();
                    CACHE.put(key, getter);
                }
            }
        }

        if (getter != null) {
            try {
                value = getter.invoke(instance);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        return value;
    }

}
