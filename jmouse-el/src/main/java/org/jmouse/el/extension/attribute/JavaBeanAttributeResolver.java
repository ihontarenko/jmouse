package org.jmouse.el.extension.attribute;

import org.jmouse.core.CacheKey;
import org.jmouse.core.Priority;
import org.jmouse.core.access.AttributeResolver;
import org.jmouse.core.access.descriptor.Describer;
import org.jmouse.core.access.descriptor.structured.ObjectDescriptor;
import org.jmouse.core.invoke.ArrayArgumentsMethodArgumentResolver;
import org.jmouse.core.invoke.InvocationRequest;
import org.jmouse.core.invoke.InvocableMethod;
import org.jmouse.core.invoke.MethodInvoker;
import org.jmouse.core.reflection.TypeInformation;
import org.jmouse.core.scope.Context;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static org.jmouse.core.Verify.nonNull;
import static org.jmouse.core.invoke.InvocationMethodContext.createDefault;

/**
 * {@link AttributeResolver} that resolves JavaBean properties via getter methods. ☕
 *
 * <p>
 * This resolver supports regular bean-style objects and invokes readable
 * property getters discovered through {@link ObjectDescriptor metadata}.
 * Resolved getter methods are cached for faster repeated access.
 * </p>
 *
 * <p>
 * Intended for EL attribute resolution against standard Java objects.
 * </p>
 */
@Priority(-10)
public class JavaBeanAttributeResolver implements AttributeResolver {

    private static final Map<CacheKey, InvocableMethod> CACHE = new HashMap<>();

    private final Context       context;
    private final MethodInvoker invoker;

    /**
     * Creates resolver with explicit invocation context and method invoker.
     *
     * @param context invocation context
     * @param invoker method invoker
     */
    public JavaBeanAttributeResolver(Context context, MethodInvoker invoker) {
        this.context = context;
        this.invoker = invoker;
    }

    /**
     * Creates resolver with default invocation infrastructure. 🧱
     */
    public JavaBeanAttributeResolver() {
        this(createDefault(), new MethodInvoker.Default(new ArrayArgumentsMethodArgumentResolver()));
    }

    /**
     * Returns whether the given instance is treated as a JavaBean.
     *
     * @param instance target instance
     *
     * @return {@code true} if bean-style property access is supported
     */
    @Override
    public boolean supports(Object instance) {
        return TypeInformation.forInstance(instance).isBean();
    }

    /**
     * Resolves a bean property by invoking its getter method.
     *
     * <p>
     * The getter is looked up from descriptor metadata and cached using
     * the instance/name pair. If the property is not readable, resolution
     * fails with a null-check exception.
     * </p>
     *
     * @param instance target bean instance
     * @param name     property name
     *
     * @return resolved property value
     */
    @Override
    public Object resolve(Object instance, String name) {
        InvocableMethod getter = null;

        if (instance != null) {
            Class<?> type = instance.getClass();
            CacheKey key  = CacheKey.of(instance, name);

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