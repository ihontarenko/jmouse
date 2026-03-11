package org.jmouse.action.support;

import org.jmouse.action.ActionHandler;
import org.jmouse.action.ActionRegistry;
import org.jmouse.action.MethodActionHandlerAdapter;
import org.jmouse.action.annotation.Action;
import org.jmouse.core.annotation.AnnotationProcessingContext;
import org.jmouse.core.annotation.support.AbstractMethodAnnotationProcessor;
import org.jmouse.core.invoke.InvocableMethod;
import org.jmouse.core.invoke.MethodInvoker;
import org.jmouse.core.reflection.Reflections;

import java.lang.reflect.Method;

import static org.jmouse.core.Verify.nonNull;

/**
 * {@link org.jmouse.core.annotation.AnnotationProcessor} that registers
 * {@link Action}-annotated methods in the {@link ActionRegistry}. 🚀
 *
 * <p>
 * For each discovered {@link Action} method, an {@link InvocableMethod}
 * is created and wrapped into {@link MethodActionHandlerAdapter},
 * allowing the action subsystem to invoke the method via
 * {@link MethodInvoker}.
 * </p>
 */
public abstract class ActionAnnotationProcessor extends AbstractMethodAnnotationProcessor<Action> {

    private final ActionRegistry registry;
    private final MethodInvoker  methodInvoker;

    /**
     * Creates processor with required infrastructure.
     *
     * @param registry      action registry
     * @param methodInvoker method invoker
     */
    protected ActionAnnotationProcessor(ActionRegistry registry, MethodInvoker methodInvoker) {
        super(Action.class);
        this.methodInvoker = nonNull(methodInvoker, "methodInvoker");
        this.registry = nonNull(registry, "registry");
    }

    /**
     * Registers the annotated method as an action handler.
     */
    @Override
    public void process(Method method, Action annotation, Class<?> declaringClass, AnnotationProcessingContext context) {
        Object          target    = resolveTarget(method, annotation, context);
        InvocableMethod invocable = new InvocableMethod(target, method);
        ActionHandler   adapter   = new MethodActionHandlerAdapter(invocable, methodInvoker);

        registry.register(annotation.value(), adapter);
    }

    /**
     * Resolves the target instance for the action method.
     *
     * @param method      action method
     * @param annotation  action annotation
     * @param context     processing context
     *
     * @return method target instance
     */
    protected abstract Object resolveTarget(Method method, Action annotation, AnnotationProcessingContext context);

    /**
     * Simple reflection-based {@link ActionAnnotationProcessor}. 🧱
     *
     * <p>
     * Instantiates the declaring class using its first constructor.
     * </p>
     */
    public static class Default extends ActionAnnotationProcessor {

        public Default(ActionRegistry registry, MethodInvoker methodInvoker) {
            super(registry, methodInvoker);
        }

        /**
         * Creates a new instance of the declaring class via reflection.
         */
        @Override
        protected Object resolveTarget(Method method, Action annotation, AnnotationProcessingContext context) {
            Class<?> declaringType = method.getDeclaringClass();
            return Reflections.instantiate(Reflections.findFirstConstructor(declaringType));
        }
    }

}