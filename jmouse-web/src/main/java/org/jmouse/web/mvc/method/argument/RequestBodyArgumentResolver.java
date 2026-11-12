package org.jmouse.web.mvc.method.argument;

import java.io.IOException;
import org.jmouse.core.MediaType;
import org.jmouse.core.MethodParameter;
import org.jmouse.http.HttpStatus;
import org.jmouse.validator.Hints;
import org.jmouse.validator.ValidationProcessor;
import org.jmouse.web.annotation.RequestBody;
import org.jmouse.web.binding.SimpleBindingResult;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.factories.WebParametersDataBinderConfiguration;
import org.jmouse.web.http.RequestContext;
import org.jmouse.web.http.converter.*;
import org.jmouse.web.mvc.ArgumentResolverException;
import org.jmouse.web.mvc.HttpRequestException;
import org.jmouse.web.mvc.MappingResult;
import org.jmouse.web.mvc.method.AbstractArgumentResolver;

public final class RequestBodyArgumentResolver extends AbstractArgumentResolver {

    private static final String CONSUMED_ATTRIBUTE = RequestBodyArgumentResolver.class.getName();
    private MessageConverterManager converters;
    private ValidationProcessor validation;

    @Override
    public void doInitialize(WebBeanContext context) {
        converters = context.getBean(MessageConverterManager.class);
        validation = context.getBean(
            ValidationProcessor.class,
            WebParametersDataBinderConfiguration.VALIDATION_PROCESSOR
        );
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameter().isAnnotationPresent(RequestBody.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, RequestContext context, MappingResult mapping) {
        RequestBody annotation = parameter.getParameter().getAnnotation(RequestBody.class);
        if (!(parameter.getParameter().getParameterizedType() instanceof Class<?> target)) {
            throw new ArgumentResolverException(
                "RequestBody requires a concrete DTO/record type; use a custom resolver for generic bodies"
            );
        }
        if (Boolean.TRUE.equals(context.request().getAttribute(CONSUMED_ATTRIBUTE))) {
            throw new ArgumentResolverException("Only one RequestBody argument is allowed per handler");
        }
        context.request().setAttribute(CONSUMED_ATTRIBUTE, true);
        try {
            ServletHttpInputMessage input = new ServletHttpInputMessage(
                context.request(),
                annotation.maxBytes()
            );
            if (!input.hasBody()) {
                if (annotation.required() || target.isPrimitive()) {
                    throw new HttpRequestException(
                        HttpStatus.BAD_REQUEST,
                        "Required request body is missing"
                    );
                }
                return BindingResultArgumentSupport.expose(
                    new SimpleBindingResult<>(
                        null,
                        new org.jmouse.validator.DefaultErrorsFactory().create(
                            null,
                            parameter.getParameter().getName()
                        )
                    ),
                    parameter,
                    context
                );
            }
            MediaType mediaType = input.getHeaders().getContentType();
            if (mediaType == null) {
                mediaType = MediaType.APPLICATION_OCTET_STREAM;
            }
            HttpMessageConverter<Object> converter = converters.getReadableMessageConverter(
                target,
                mediaType
            );
            if (converter == null) {
                throw new HttpRequestException(
                    HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "No converter can read this request body"
                );
            }
            Object body = converter.read(target, input);
            input.discardRemaining();
            if (body == null && annotation.required()) {
                throw new HttpRequestException(
                    HttpStatus.BAD_REQUEST,
                    "Required request body resolved to null"
                );
            }
            if (body == null) {
                return BindingResultArgumentSupport.expose(
                    new SimpleBindingResult<>(
                        null,
                        new org.jmouse.validator.DefaultErrorsFactory().create(
                            null,
                            parameter.getParameter().getName()
                        )
                    ),
                    parameter,
                    context
                );
            }
            var result = validation.validate(body, parameter.getParameter().getName(), Hints.empty());
            return BindingResultArgumentSupport.expose(
                new SimpleBindingResult<>(body, result.errors()),
                parameter,
                context
            );
        } catch (IOException | UnreadableException failure) {
            java.util.Set<Throwable> visited = java.util.Collections.newSetFromMap(
                new java.util.IdentityHashMap<>()
            );
            for (Throwable cause = failure; cause != null && visited.add(cause); cause = cause.getCause()) {
                if (cause instanceof HttpRequestException requestFailure) {
                    throw requestFailure;
                }
            }
            throw new HttpRequestException(HttpStatus.BAD_REQUEST, "Cannot read request body", failure);
        }
    }
}
