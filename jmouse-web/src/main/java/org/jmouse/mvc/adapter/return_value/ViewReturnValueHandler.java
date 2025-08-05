package org.jmouse.mvc.adapter.return_value;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.core.reflection.annotation.AnnotationRepository;
import org.jmouse.core.reflection.annotation.MergedAnnotation;
import org.jmouse.mvc.InvocationResult;
import org.jmouse.mvc.MethodParameter;
import org.jmouse.mvc.adapter.AbstractReturnValueHandler;
import org.jmouse.mvc.mapping.annnotation.ViewMapping;
import org.jmouse.web.context.WebBeanContext;

import java.util.Optional;

public class ViewReturnValueHandler extends AbstractReturnValueHandler {

    public static final String VIEW_PREFIX = "view:";

    @Override
    protected void doInitialize(WebBeanContext context) {

    }

    @Override
    protected void doReturnValueHandle(InvocationResult result, HttpServletRequest request, HttpServletResponse response) {

    }

    @Override
    public boolean supportsReturnType(MethodParameter returnType, InvocationResult result) {
        Optional<MergedAnnotation> optional    = AnnotationRepository.ofAnnotatedElement(returnType.getExecutable())
                .get(ViewMapping.class);
        Object                     returnValue = result.getReturnValue();

        if (optional.isPresent()) {
            return true;
        } else if (returnValue instanceof String viewName) {
            return viewName.startsWith(VIEW_PREFIX);
        }

        return false;
    }

}
