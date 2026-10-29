package org.jmouse.cad.management;

import org.jmouse.cad.jpa.catalogue.DocumentOwner;
import org.jmouse.cad.store.problem.CadProblem;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

/**
 * 🔗 Joins the two path segments an {@link OwnerAddress} names into the owner they spell.
 *
 * <p>Read from the template variables the handler mapping already matched, so the segments arrive decoded exactly as
 * an ordinary {@code @PathVariable} would.</p>
 */
final class OwnerAddressArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(OwnerAddress.class) && parameter.getParameterType() == String.class;
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer container, NativeWebRequest request,
                                  WebDataBinderFactory binders) {
        OwnerAddress address   = parameter.getParameterAnnotation(OwnerAddress.class);
        Map<?, ?>    variables = templateVariables(request);
        Object       type      = variables.get(address.type());
        Object       id        = variables.get(address.id());

        if (!(type instanceof String ownerType) || ownerType.isBlank()
                || !(id instanceof String ownerId) || ownerId.isBlank()) {
            throw CadProblem.invalid("The address names no owner.");
        }

        return DocumentOwner.of(ownerType, ownerId).toString();
    }

    /**
     * 🏷️ The owner an {@link OwnerAddress} argument carries, back as the record the services take.
     *
     * @param address the argument, as this resolver joined it
     * @return the owner
     */
    static DocumentOwner ownerOf(String address) {
        return DocumentOwner.parse(address).orElseThrow(() -> CadProblem.invalid("The address names no owner."));
    }

    private static Map<?, ?> templateVariables(NativeWebRequest request) {
        Object variables = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE,
                                                RequestAttributes.SCOPE_REQUEST);

        if (variables instanceof Map<?, ?> matched) {
            return matched;
        }

        return Map.of();
    }
}
