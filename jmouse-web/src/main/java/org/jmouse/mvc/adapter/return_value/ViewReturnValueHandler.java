package org.jmouse.mvc.adapter.return_value;

import org.jmouse.core.reflection.annotation.AnnotationRepository;
import org.jmouse.core.reflection.annotation.MergedAnnotation;
import org.jmouse.mvc.*;
import org.jmouse.mvc.adapter.AbstractReturnValueHandler;
import org.jmouse.mvc.mapping.annnotation.ViewMapping;
import org.jmouse.web.context.WebBeanContext;

import java.util.Optional;

/**
 * 🖼️ Handles return values that resolve to views.
 *
 * <p>This handler supports controller methods that either:
 * <ul>
 *   <li>Are annotated with {@link ViewMapping}</li>
 *   <li>Return a {@code String} with a {@code "view:"} prefix</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * @ViewMapping
 * public String home(Model model) {
 *     model.add("user", currentUser);
 *     return "view:home";
 * }
 * }</pre>
 *
 * <p>The returned string is passed to the {@link ViewResolver} to resolve
 * and render the corresponding view.
 *
 * @see View
 * @see ViewResolver
 * @see AbstractReturnValueHandler
 * @see ViewMapping
 */
public class ViewReturnValueHandler extends AbstractReturnValueHandler {

    /** Prefix used to identify view-returning methods via string return values. */
    public static final String VIEW_PREFIX = "view:";

    private ViewResolver viewResolver;

    /**
     * Initializes the view resolver from the application context.
     *
     * @param context the web bean context
     */
    @Override
    protected void doInitialize(WebBeanContext context) {
        viewResolver = context.getBean(ViewResolver.class);
    }

    /**
     * Processes the return value by resolving and rendering the associated view.
     *
     * @param result         the invocation outcome containing model and return value
     * @param requestContext the current HTTP request/response context
     */
    @Override
    protected void doReturnValueHandle(InvocationOutcome result, RequestContext requestContext) {
        View view = viewResolver.resolveView((String) result.getReturnValue());
        view.render(result.getModel().getAttributes(), requestContext.request(), requestContext.response());
        result.setState(ExecutionState.HANDLED);
    }

    /**
     * Checks if the return type is supported.
     *
     * <p>This includes methods annotated with {@link ViewMapping}
     * or returning a {@code String} starting with {@code "view:"}.
     *
     * @param returnType the method return type metadata
     * @param outcome    the actual method result
     * @return {@code true} if handled by this strategy
     */
    @Override
    public boolean supportsReturnType(MethodParameter returnType, InvocationOutcome outcome) {
        Optional<MergedAnnotation> optional = AnnotationRepository
                .ofAnnotatedElement(returnType.getAnnotatedElement())
                .get(ViewMapping.class);

        Object returnValue = outcome.getReturnValue();

        if (optional.isPresent()) {
            return true;
        } else if (returnValue instanceof String viewName) {
            return viewName.startsWith(VIEW_PREFIX);
        }

        return false;
    }
}
