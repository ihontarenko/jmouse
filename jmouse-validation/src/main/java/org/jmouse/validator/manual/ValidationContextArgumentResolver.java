package org.jmouse.validator.manual;

import org.springframework.core.MethodParameter;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.annotation.ErrorsMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;


public class ValidationContextArgumentResolver implements HandlerMethodArgumentResolver {

    private final ErrorsMethodArgumentResolver errorsMethodArgumentResolver = new ErrorsMethodArgumentResolver();

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return ValidationContext.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        ValidationContext context = new ValidationContext.Simple();
        Object            errors  = null;

        try {
            errors = errorsMethodArgumentResolver.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
        } catch (IllegalStateException ignore) {
        }

        if (errors instanceof BindingResult bindingResult) {
            context.setAttribute(ValidationContext.BINDING_RESULT_KEY, bindingResult);
        }

        context.setAttribute(MethodParameter.class, parameter);
        context.setAttribute(ModelAndViewContainer.class, mavContainer);
        context.setAttribute(NativeWebRequest.class, webRequest);
        context.setAttribute(WebDataBinderFactory.class, binderFactory);

        return context;
    }

}
