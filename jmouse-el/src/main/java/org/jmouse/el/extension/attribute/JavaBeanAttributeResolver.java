package org.jmouse.el.extension.attribute;

import org.jmouse.core.access.AttributeResolver;
import org.jmouse.core.access.descriptor.Describer;
import org.jmouse.core.access.descriptor.structured.ObjectDescriptor;
import org.jmouse.core.invoke.*;
import org.jmouse.core.reflection.TypeInformation;
import org.jmouse.core.Priority;
import org.jmouse.core.scope.Context;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.jmouse.core.Verify.nonNull;
import static org.jmouse.core.invoke.InvocationMethodContext.createDefault;

@Priority(-10)
public class JavaBeanAttributeResolver implements AttributeResolver {

    private static final Map<String, InvocableMethod> CACHE = new HashMap<>();

    private final Context       context;
    private final MethodInvoker invoker;

    public JavaBeanAttributeResolver(Context context, MethodInvoker invoker) {
        this.context = context;
        this.invoker = invoker;
    }

    public JavaBeanAttributeResolver() {
        this(createDefault(), new MethodInvoker.Default(new ArrayArgumentsMethodArgumentResolver()));
    }

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
        InvocableMethod getter = null;

        if (instance != null) {
            Class<?> type = instance.getClass();
            String   key  = type.getName() + ":" + name;

            getter = CACHE.get(key);

            if (getter == null) {
                ObjectDescriptor<?> descriptor = Describer.forObjectDescriptor(type);
                if (descriptor.hasProperty(name) && descriptor.getProperty(name).isReadable()) {
                    Method method = descriptor.getProperty(name).getGetterMethod().unwrap();
                    getter = new InvocableMethod(instance, method);
                    CACHE.put(key, getter);
                }
            }
        }

        return invoker.invoke(new InvocationRequest.Default(nonNull(getter, "getter"), context));
    }

}
