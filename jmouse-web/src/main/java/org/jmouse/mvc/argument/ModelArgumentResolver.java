package org.jmouse.mvc.argument;

import org.jmouse.mvc.*;
import org.jmouse.web.context.WebBeanContext;

/**
 * 🧩 Resolver for {@link Model} method parameters.
 *
 * <p>Injects the current {@link Model} instance from the {@link InvocationOutcome}
 * into controller method parameters.
 *
 * <pre>{@code
 * @Controller
 * public class UserController {
 *
 *     @Get("/user")
 *     public String showUser(Model model) {
 *         model.setAttribute("user", ...);
 *         return "userView";
 *     }
 * }
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public class ModelArgumentResolver extends AbstractArgumentResolver {

    /**
     * ⚙️ Initialization logic for this resolver (unused here).
     *
     * @param context the current web bean context
     */
    @Override
    protected void doInitialize(WebBeanContext context) {
        // NO-OP
    }

    /**
     * ✅ Supports parameters of type {@link Model}.
     *
     * @param parameter the method parameter to check
     * @return true if the parameter is assignable to Model
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.isParameter() && parameter.getParameterType() == Model.class;
    }

    /**
     * 📦 Returns the {@link Model} instance from the current invocation result.
     *
     * @param parameter the target parameter
     * @param mappingResult the mapping result
     * @param invocationResult the result container of the handler
     * @return the current Model instance
     */
    @Override
    public Object resolveArgument(
            MethodParameter parameter, MappingResult mappingResult, InvocationOutcome invocationResult) {
        return invocationResult.getModel();
    }
}
