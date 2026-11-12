package org.jmouse.web.mvc.result;

import static org.jmouse.core.reflection.annotation.AnnotationRepository.ofAnnotatedElement;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.jmouse.beans.InitializingBeanSupport;
import org.jmouse.core.MethodParameter;
import org.jmouse.core.Priority;
import org.jmouse.core.reflection.annotation.MergedAnnotation;
import org.jmouse.web.annotation.ViewMapping;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.http.RequestContext;
import org.jmouse.web.mvc.RequestModel;
import org.jmouse.web.mvc.View;
import org.jmouse.web.mvc.ViewResolver;

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
 * // prioritized
 * @ViewMapping("index/home")
 * public String home(Model model) {
 *     model.add("user", currentUser);
 *     // or return prefixed view-name
 *     return "view:home";
 * }
 * }</pre>
 *
 * <p>The returned string is passed to the {@link ViewResolver} to resolve
 * and render the corresponding view.
 *
 * @see View
 * @see ViewResolver
 * @see ReturnValueHandler
 * @see ViewMapping
 */
@Priority(0)
public class ViewResolverReturnValueHandler
    implements ReturnValueHandler, InitializingBeanSupport<WebBeanContext>
{

    /** Prefix used to identify view-returning methods via string return values. */
    public static final String VIEW_PREFIX = "view:";

    private ViewResolver viewResolver;

    /**
     * Initializes the view resolver from the application context.
     *
     * @param context the web bean context
     */
    @Override
    public void doInitialize(WebBeanContext context) {
        viewResolver = context.getBean(ViewResolver.class);
    }

    /**
     * 🧪 Handles the return value when it represents a view to be rendered.
     *
     * <p>Supports two strategies:
     * <ul>
     *     <li>🔍 If the method is annotated with {@link ViewMapping}, the view name is resolved from the annotation.</li>
     *     <li>📝 If the return value is a {@code String} starting with {@code view:}, it's treated as a direct view name.</li>
     * </ul>
     *
     * <p>If a view name is resolved, it's rendered via the configured {@link ViewResolver}.
     */
    @Override
    public void handleReturnValue(HandlerResult.ReturnValue result, RequestContext requestContext) {
        MethodParameter returnType = result.returnType();
        Optional<MergedAnnotation> optional = ofAnnotatedElement(returnType.getAnnotatedElement()).get(
            ViewMapping.class
        );
        String viewName = null;
        HttpServletResponse response = requestContext.response();

        if (optional.isPresent()) {
            ViewMapping mapping = optional.get().synthesize();
            viewName = mapping.name();
        } else if (result.value() instanceof String name && name.startsWith(VIEW_PREFIX)) {
            viewName = name.substring(VIEW_PREFIX.length());
        }

        if (viewName != null && !viewName.isBlank()) {
            try {
                View view = viewResolver.resolveView(viewName);
                response.setContentType(view.getContentType().toString());
                view.render(
                    RequestModel.get(requestContext.request()).getAttributes(),
                    requestContext.request(),
                    response
                );
            } catch (Exception e) {
                throw new ViewRenderingException("Could not render view: " + viewName, e);
            }
        } else {
            throw new ViewRenderingException("Unable to resolve view");
        }
    }

    /**
     * Checks if the return type is supported.
     *
     * <p>This includes methods annotated with {@link ViewMapping}
     * or returning a {@code String} starting with {@code "view:"}.
     *
     * @param result    the actual method result
     * @return {@code true} if handled by this strategy
     */
    @Override
    public boolean supportsReturnType(HandlerResult.ReturnValue result) {
        Object returnValue = result.value();
        MethodParameter returnType = result.returnType();

        if (returnType != null) {
            Optional<MergedAnnotation> optional = ofAnnotatedElement(returnType.getAnnotatedElement()).get(
                ViewMapping.class
            );

            if (optional.isPresent()) {
                return true;
            }

            if (returnValue instanceof String viewName) {
                return viewName.startsWith(VIEW_PREFIX);
            }

            return returnValue instanceof View;
        }

        return false;
    }
}
