package org.jmouse.action.support;

import org.jmouse.action.ActionRegistry;
import org.jmouse.action.MethodActionHandlerAdapter;
import org.jmouse.action.annotation.Action;
import org.jmouse.core.Verify;
import org.jmouse.core.annotation.AnnotationProcessingContext;
import org.jmouse.core.annotation.support.AbstractMethodAnnotationProcessor;
import org.jmouse.core.invoke.InvocableMethod;
import org.jmouse.core.invoke.MethodInvoker;
import org.jmouse.core.reflection.Reflections;

import java.lang.reflect.Method;

/**
 * Registers {@link Action}-annotated methods into {@link ActionRegistry}. 🚀
 */
public abstract class ActionAnnotationProcessor extends AbstractMethodAnnotationProcessor<Action> {

    private final ActionRegistry actionRegistry;
    private final MethodInvoker  methodInvoker;

    protected ActionAnnotationProcessor(ActionRegistry actionRegistry, MethodInvoker methodInvoker) {
        super(Action.class);
        this.actionRegistry = Verify.nonNull(actionRegistry, "registry");
        this.methodInvoker = Verify.nonNull(methodInvoker, "methodInvoker");
    }

    @Override
    public void process(Method method, Action annotation, Class<?> declaringClass, AnnotationProcessingContext context) {
        Object          target    = resolveTarget(method, annotation, context);
        InvocableMethod invocable = new InvocableMethod(target, method);

        actionRegistry.register(
                annotation.value(),
                new MethodActionHandlerAdapter(invocable, methodInvoker)
        );
    }

    protected abstract Object resolveTarget(Method method, Action annotation, AnnotationProcessingContext context);

    /**
     * Simple reflection-based action annotation processor. 🧱
     */
    public static class Default extends ActionAnnotationProcessor {

        public Default(ActionRegistry registry, MethodInvoker methodInvoker) {
            super(registry, methodInvoker);
        }

        @Override
        protected Object resolveTarget(Method method, Action annotation, AnnotationProcessingContext context) {
            Class<?> declaringType = method.getDeclaringClass();
            return Reflections.instantiate(Reflections.findFirstConstructor(declaringType));
        }
    }

}