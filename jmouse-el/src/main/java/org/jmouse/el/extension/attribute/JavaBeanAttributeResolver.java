package org.jmouse.el.extension.attribute;

import org.jmouse.core.bind.AttributeResolver;
import org.jmouse.core.bind.descriptor.Describer;
import org.jmouse.core.bind.descriptor.structured.jb.JavaBeanDescriptor;
import org.jmouse.core.bind.descriptor.structured.jb.JavaBeanIntrospector;
import org.jmouse.core.reflection.TypeInformation;
import org.jmouse.util.Priority;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@Priority(-10)
public class JavaBeanAttributeResolver implements AttributeResolver {

    private static final Map<String, Method> CACHE = new HashMap<>();

    /**
     * Determines whether this resolver can handle the specified instance.
     *
     * @param instance the target object instance to check
     * @return {@code true} if this resolver supports resolving attributes on the instance;
     * {@code false} otherwise
     */
    @Override
    public boolean supports(Object instance) {
        return TypeInformation.forInstance(instance).isBean();
    }

    @Override
    public Object resolve(Object instance, String name) {
        Method getter = null;
        Object value  = null;

        if (instance != null) {
            Class<?> clazz = instance.getClass();
            String   key   = clazz.getName() + "." + name;

            getter = CACHE.get(key);

            if (getter == null) {
                JavaBeanDescriptor<?> descriptor = Describer.forJavaBean(clazz);
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
