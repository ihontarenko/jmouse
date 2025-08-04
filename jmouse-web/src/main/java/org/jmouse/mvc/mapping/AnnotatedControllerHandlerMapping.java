package org.jmouse.mvc.mapping;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.beans.definition.BeanDefinition;
import org.jmouse.core.MediaType;
import org.jmouse.core.reflection.MethodFinder;
import org.jmouse.core.reflection.MethodMatchers;
import org.jmouse.core.reflection.annotation.MergedAnnotation;
import org.jmouse.core.reflection.annotation.AnnotationRepository;
import org.jmouse.mvc.AbstractHandlerPathMapping;
import org.jmouse.mvc.HandlerMethod;
import org.jmouse.mvc.MappedHandler;
import org.jmouse.mvc.Route;
import org.jmouse.mvc.mapping.annnotation.*;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.request.http.HttpHeader;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Optional;

import static org.jmouse.util.Streamable.of;

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
     * 📌 Matches an incoming request to a registered handler method.
     *
     * @param request incoming HTTP request
     * @return matched handler or {@code null}
     */
    @Override
    protected MappedHandler doGetHandler(HttpServletRequest request) {
        return null;
    }

    /**
     * 📌 Scans local beans for {@link Controller}-annotated classes and registers handler methods.
     *
     * @param context active web context
     */
    @Override
    protected void doInitialize(WebBeanContext context) {
        for (String beanName : context.getBeanNames(Object.class)) {
            if (context.isLocalBean(beanName)) {
                BeanDefinition definition = context.getDefinition(beanName);
                if (definition.isAnnotatedWith(Controller.class)) {
                    Object bean = context.getBean(definition.getBeanName());
                    Collection<Method> methods = new MethodFinder().find(
                            definition.getBeanClass(), MethodMatchers.isPublic());
                    initializeMethods(methods, bean, context);
                }
            }
        }
    }

    /**
     * 📌 Extracts {@link Mapping} from each method and registers it with its route.
     *
     * @param methods controller methods
     * @param bean controller instance
     */
    private void initializeMethods(Collection<Method> methods, Object bean, WebBeanContext context) {
        for (Method method : methods) {
            AnnotationRepository       repository = AnnotationRepository.ofAnnotatedElement(method);
            Optional<MergedAnnotation> optional   = repository.get(Mapping.class);

            if (optional.isPresent()) {
                MergedAnnotation annotation = optional.get();
                Mapping          mapping    = annotation.createSynthesizedAnnotation(Mapping.class);
                Route            route      = createRoute(mapping);

                addHandlerMapping(route, new HandlerMethod(context, bean, method));
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

        // Add expected headers
        for (Header header : mapping.headers()) {
            builder.header(HttpHeader.ofHeader(header.name()), header.value());
        }

        // Add expected query parameters
        for (QueryParameter queryParameter : mapping.queryParameters()) {
            builder.queryParameter(queryParameter.name(), queryParameter.value());
        }

        return builder.build();
    }
}
