package org.jmouse.mvc.mapping;

import org.jmouse.core.MediaType;
import org.jmouse.core.reflection.annotation.MergedAnnotation;
import org.jmouse.core.reflection.annotation.AnnotationRepository;
import org.jmouse.mvc.*;
import org.jmouse.web.annotation.Controller;
import org.jmouse.web.annotation.Header;
import org.jmouse.web.annotation.Mapping;
import org.jmouse.web.annotation.QueryParameter;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.method.HandlerMethod;
import org.jmouse.core.MethodParameter;
import org.jmouse.web.http.HttpHeader;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Optional;

import static org.jmouse.core.Streamable.of;

/**
 * 📌 Detects controller beans and registers their annotated handler methods.
 *
 * <p>Supports {@link Controller} and {@link Mapping} annotations.
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class AnnotatedControllerHandlerMapping extends AbstractHandlerPathMapping<HandlerMethod> {

    /**
     * 📌 Scans local beans for {@link Controller}-annotated classes and registers handler methods.
     *
     * @param context active web context
     */
    @Override
    protected void doInitialize(WebBeanContext context) {
        WebBeanContext.methodsOfAnnotatedClasses(Controller.class, this::initializeMethods, context);
    }

    /**
     * 📌 Extracts {@link Mapping} from each method and registers it with its route.
     *
     * @param methods controller methods
     * @param bean controller instance
     */
    private void initializeMethods(Collection<Method> methods, Object bean) {
        for (Method method : methods) {
            AnnotationRepository       repository = AnnotationRepository.ofAnnotatedElement(method);
            Optional<MergedAnnotation> mapping    = repository.get(Mapping.class);
            if (mapping.isPresent()) {
                Mapping annotation = mapping.get().synthesize();
                Route   route      = createRoute(annotation);
                addHandlerMapping(route, new HandlerMethod(bean, method));
            }
        }
    }

    /**
     * 📌 Builds a {@link Route} from the {@link Mapping} annotation.
     *
     * @param mapping mapping metadata
     * @return constructed route
     */
    private Route createRoute(Mapping mapping) {
        Route.Builder builder = Route.route()
                .method(mapping.httpMethod())
                .path(mapping.path())
                .consumes(of(mapping.consumes())
                                  .map(MediaType::forString).toList().toArray(MediaType[]::new))
                .produces(of(mapping.produces())
                                  .map(MediaType::forString).toList().toArray(MediaType[]::new));

        for (Header header : mapping.headers()) {
            builder.header(HttpHeader.ofHeader(header.name()), header.value());
        }

        for (QueryParameter queryParameter : mapping.queryParameters()) {
            builder.queryParameter(queryParameter.name(), queryParameter.value());
        }

        return builder.build();
    }

    /**
     * ✅ Checks whether the given mapped handler is a {@link HandlerMethod}.
     *
     * <p>This is used to determine if the current component can handle
     * the provided handler type during request dispatching.
     *
     * @param mapped the mapped handler object to check
     * @return {@code true} if it is a {@link HandlerMethod}, {@code false} otherwise
     */
    @Override
    public boolean supportsMappedHandler(Object mapped) {
        return mapped instanceof HandlerMethod;
    }

    /**
     * 📦 Resolves the return type parameter of the given handler method.
     *
     * <p>Always resolves with {@code index = -1}, which refers to the method return type.
     *
     * @param handler the handler method to resolve from
     * @return a {@link MethodParameter} representing the method's return type
     */
    @Override
    protected MethodParameter getReturnParameter(HandlerMethod handler) {
        return MethodParameter.forMethod(handler.getMethod(), -1);
    }
}
