package org.jmouse.mvc.exception;

import org.jmouse.core.reflection.annotation.AnnotationRepository;
import org.jmouse.core.reflection.annotation.MergedAnnotation;
import org.jmouse.mvc.*;
import org.jmouse.mvc.mapping.annnotation.Controller;
import org.jmouse.mvc.mapping.annnotation.ExceptionHandler;
import org.jmouse.web.context.WebBeanContext;

import java.lang.reflect.Method;
import java.util.*;

public class ExceptionHandlerExceptionResolver extends AbstractExceptionResolver {

    private final Set<Class<? extends Throwable>>                         supportedExceptions;
    private final Map<Class<? extends Throwable>, ExceptionHandlerMethod> exceptionMappings;

    public ExceptionHandlerExceptionResolver() {
        this.supportedExceptions = new HashSet<>();
        this.exceptionMappings = new HashMap<>();
    }

    @Override
    public boolean supportsException(Throwable exception) {
        return supportedExceptions.contains(exception.getClass());
    }

    @Override
    protected void doInitialize(WebBeanContext context) {
        WebBeanContext.selectMethods(Controller.class, this::initializeMethods, context);
    }

    private void initializeMethods(Collection<Method> methods, Object bean) {
        for (Method method : methods) {
            AnnotationRepository       repository       = AnnotationRepository.ofAnnotatedElement(method);
            Optional<MergedAnnotation> exceptionHandler = repository.get(ExceptionHandler.class);
            if (exceptionHandler.isPresent()) {
                ExceptionHandler             annotation     = exceptionHandler.get().synthesize();
                Class<? extends Throwable>[] exceptionTypes = annotation.value();
                for (Class<? extends Throwable> exceptionType : exceptionTypes) {
                    exceptionMappings.put(exceptionType, new ExceptionHandlerMethod(exceptionType, bean, method));
                    supportedExceptions.add(exceptionType);
                }
            }
        }
    }

    @Override
    protected InvocationOutcome doExceptionResolve(RequestContext requestContext, MappedHandler mappedHandler, Exception exception) {
        InvocationOutcome outcome       = new Outcome(null);
        HandlerMethod     handlerMethod = null;

        HandlerMethodContext context = new HandlerMethodContext(requestContext, handlerMethod);
        HandlerMethodInvocation invocation = new HandlerMethodInvocation(context, mappedHandler.mappingResult(),
                                                                         outcome, null);

        return null;
    }

}
