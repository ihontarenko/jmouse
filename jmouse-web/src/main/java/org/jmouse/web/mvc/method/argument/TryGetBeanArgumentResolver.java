package org.jmouse.web.mvc.method.argument;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeanContextAware;
import org.jmouse.beans.annotation.Qualifier;
import org.jmouse.beans.resolve.BeanResolutionRequest;
import org.jmouse.beans.resolve.BeanResolutionStrategies;
import org.jmouse.beans.resolve.BeanResolutionStrategy;
import org.jmouse.core.MethodParameter;
import org.jmouse.web.http.RequestContext;
import org.jmouse.web.mvc.MappingResult;
import org.jmouse.web.mvc.method.AbstractArgumentResolver;

import java.lang.reflect.Parameter;

/**
 * Argument resolver that attempts to resolve method parameters from {@link BeanContext}. 🧩
 *
 * <p>Supports any parameter type for which at least one bean is registered
 * in the {@link BeanContext}.</p>
 *
 * <p>Resolution strategy:</p>
 * <ul>
 *     <li>If {@link Qualifier} is present → resolves bean by qualifier name</li>
 *     <li>Otherwise → resolves bean by parameter type</li>
 * </ul>
 */
public class TryGetBeanArgumentResolver extends AbstractArgumentResolver implements BeanContextAware {

    private       BeanContext            beanContext;
    private final BeanResolutionStrategy strategy = BeanResolutionStrategies.defaultStrategy();

    /**
     * Checks whether the parameter can be resolved from {@link BeanContext}. 🔎
     *
     * @param parameter method parameter descriptor
     *
     * @return {@code true} if at least one bean of the parameter type exists
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        Parameter source = parameter.getParameter();
        return strategy.supports(
                BeanResolutionRequest.forParameter(getBeanContext(), source)
        );

//        return !beanContext.getBeans(parameter.getParameterType()).isEmpty();
    }

    /**
     * Resolves the argument from {@link BeanContext}. ⚙️
     *
     * <p>If {@link Qualifier} is present on the parameter, resolves by name.
     * Otherwise resolves by type.</p>
     *
     * @param methodParameter parameter metadata
     * @param requestContext  current request context
     * @param mappingResult   mapping result (not used)
     *
     * @return resolved bean instance
     */
    @Override
    public Object resolveArgument(MethodParameter methodParameter, RequestContext requestContext, MappingResult mappingResult) {
        return strategy.resolve(
                BeanResolutionRequest.forParameter(getBeanContext(), methodParameter.getParameter())
        );

//        Parameter   parameter = methodParameter.getParameter();
//        Qualifier   qualifier = parameter.getAnnotation(Qualifier.class);
//        BeanContext context   = getBeanContext();
//
//        if (qualifier == null) {
//            return context.getBean(methodParameter.getParameterType());
//        }
//
//        return context.getBean(qualifier.value());
    }

    /**
     * Injects {@link BeanContext}. 🔌
     *
     * @param context bean context
     */
    @Override
    public void setBeanContext(BeanContext context) {
        this.beanContext = context;
    }

    /**
     * Returns the current {@link BeanContext}. 📦
     *
     * @return bean context
     */
    @Override
    public BeanContext getBeanContext() {
        return beanContext;
    }

}
