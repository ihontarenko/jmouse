package org.jmouse.mvc.adapter.return_value;

import org.jmouse.core.reflection.annotation.AnnotationRepository;
import org.jmouse.core.reflection.annotation.MergedAnnotation;
import org.jmouse.mvc.InvocationOutcome;
import org.jmouse.mvc.MethodParameter;
import org.jmouse.mvc.RequestContext;
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
    protected void doReturnValueHandle(InvocationOutcome result, RequestContext requestContext) {

    }

    @Override
    public boolean supportsReturnType(MethodParameter returnType, InvocationOutcome outcome) {
        Optional<MergedAnnotation> optional    = AnnotationRepository.ofAnnotatedElement(returnType.getExecutable())
                .get(ViewMapping.class);
        Object                     returnValue = outcome.getReturnValue();

        if (optional.isPresent()) {
            return true;
        } else if (returnValue instanceof String viewName) {
            return viewName.startsWith(VIEW_PREFIX);
        }

        return false;
    }

}
