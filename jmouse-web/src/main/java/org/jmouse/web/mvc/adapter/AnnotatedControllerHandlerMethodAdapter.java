package org.jmouse.web.mvc.adapter;

import java.util.ArrayList;
import java.util.List;
import org.jmouse.beans.InitializingBeanSupport;
import org.jmouse.core.MethodParameter;
import org.jmouse.core.Sorter;
import org.jmouse.web.context.WebBeanContext;
import org.jmouse.web.http.RequestContext;
import org.jmouse.web.mvc.HandlerAdapter;
import org.jmouse.web.mvc.MappedHandler;
import org.jmouse.web.mvc.method.ArgumentResolver;
import org.jmouse.web.mvc.method.HandlerMethod;
import org.jmouse.web.mvc.method.HandlerMethodContext;
import org.jmouse.web.mvc.method.HandlerMethodInvocation;
import org.jmouse.web.mvc.result.HandlerResult;

/** Resolves controller arguments and invokes the selected method without rendering or shared response state. */
public final class AnnotatedControllerHandlerMethodAdapter
    implements HandlerAdapter, InitializingBeanSupport<WebBeanContext>
{

    private List<ArgumentResolver> argumentResolvers = List.of();

    @Override
    public HandlerResult handle(RequestContext context, MappedHandler handler) {
        HandlerMethod method = (HandlerMethod) handler.handler();
        var invocation = new HandlerMethodInvocation(
            new HandlerMethodContext(context, method),
            handler.mappingResult(),
            argumentResolvers
        );
        Object value = invocation.invoke();
        return HandlerResult.returned(value, MethodParameter.forMethod(method.getMethod(), -1));
    }

    @Override
    public boolean supportsHandler(MappedHandler handler) {
        return handler.handler() instanceof HandlerMethod;
    }

    @Override
    public void doInitialize(WebBeanContext context) {
        var resolvers = new ArrayList<>(context.getBeans(ArgumentResolver.class));
        Sorter.sort(resolvers);
        argumentResolvers = List.copyOf(resolvers);
    }
}
