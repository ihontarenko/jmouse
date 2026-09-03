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
     * Resolves a bean property by invoking its getter, or answers {@code null} when there is no such
     * readable property.
     *
     * <h2>⚠️ Answering null IS the contract, and this used to throw instead</h2>
     *
     * <p>{@code EvaluationContext.getValue} asks every registered resolver in turn and breaks on the
     * first non-null answer:</p>
     *
     * <pre>{@code
     * for (AttributeResolver resolver : getAttributeResolvers()) {
     *     if ((value = resolver.resolve(container, last)) != null) {
     *         break;
     *     }
     * }
     * }</pre>
     *
     * <p>This one threw a {@code NullPointerException} naming a local variable — <em>"Required value
     * must be non-null: 'getter'"</em> — which broke that loop in two ways. The message said nothing
     * about the property or the object, and, worse, <strong>the map and list resolvers registered after
     * it never got asked</strong>: a value one of them would have answered was lost to an exception
     * thrown by an earlier one.</p>
     *
     * <h2>⚠️ Why "no such property" is null rather than a failure</h2>
     *
     * <p>Two situations reach here and neither is this class's to judge: the container is {@code null},
     * so the name resolved to nothing at all and nothing has properties; or it is real and simply not a
     * bean of that shape — which is exactly when a later resolver should get its turn.</p>
     *
     * <p>A <em>typo</em> in a property name is a real mistake and deserves refusing, but it cannot be
     * told from those two at this depth: a resolver sees one candidate object and one name. Whether an
     * unknown property is an error belongs to whoever knows what was expected — a dialect, at bind,
     * against a declared shape. Refusing here would instead mean every host had to put every key of
     * every event into every context, which is a rule nobody can keep.</p>
     *
     * @param instance target bean instance, possibly {@code null}
     * @param name     property name
     *
     * @return the property's value, or {@code null} when this resolver cannot answer
     */
    @Override
    public Object resolve(Object instance, String name) {
        if (instance == null) {
            return null;
        }

        CacheKey        key    = CacheKey.of(instance, name);
        InvocableMethod getter = CACHE.get(key);

        if (getter == null) {
            ObjectDescriptor<?> descriptor = Describer.forObjectDescriptor(instance.getClass());

            if (!descriptor.hasProperty(name) || !descriptor.getProperty(name).isReadable()) {
                return null;
            }

            getter = new InvocableMethod(instance, descriptor.getProperty(name).getGetterMethod().unwrap());
            CACHE.put(key, getter);
        }

        return invoker.invoke(new InvocationRequest.Default(getter, context));
    }

}