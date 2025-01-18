package svit.web.configuration;

import svit.proxy.MethodInterceptor;
import svit.proxy.MethodInvocation;
import svit.proxy.ProxyContext;
import svit.proxy.annotation.ProxyMethodInterceptor;
import svit.web.server.WebServerFactory;

import java.lang.reflect.Method;

/**
 * A {@link MethodInterceptor} for intercepting methods on {@link WebServerFactory}.
 * <p>
 * This interceptor is annotated with {@link ProxyMethodInterceptor @ProxyMethodInterceptor},
 * indicating that it should be applied to the {@code WebServerFactory} class, allowing
 * you to inject custom behavior before, during, or after method invocations. By default,
 * all methods simply delegate to {@code super}, resulting in no additional actions.
 */
@ProxyMethodInterceptor(WebServerFactory.class)
public class WebServerFactoryInterceptor implements MethodInterceptor {

    /**
     * Invoked before the target method is called. Currently, no additional logic is added,
     * but subclasses or modified versions can override this to inject pre-processing.
     *
     * @param context   the proxy context holding information about the invocation
     * @param method    the method being invoked
     * @param arguments the arguments passed to the method
     */
    @Override
    public void before(ProxyContext context, Method method, Object[] arguments) {
        MethodInterceptor.super.before(context, method, arguments);
    }

    /**
     * The core interception logic, which delegates to the default implementation.
     * This method can be overridden to wrap the invocation (e.g., for logging or
     * transaction handling) before calling {@link MethodInvocation#proceed()}.
     *
     * @param invocation the {@link MethodInvocation} encapsulating the target method
     * @return the result of the method call
     * @throws Throwable if an error occurs during method invocation
     */
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        return MethodInterceptor.super.invoke(invocation);
    }

    /**
     * Invoked after the target method returns. Currently, no additional logic is added,
     * but subclasses or modified versions can override this for post-processing, logging,
     * or handling the result.
     *
     * @param context   the proxy context holding information about the invocation
     * @param method    the method that was invoked
     * @param arguments the arguments passed to the method
     * @param result    the result returned by the method
     */
    @Override
    public void after(ProxyContext context, Method method, Object[] arguments, Object result) {
        MethodInterceptor.super.after(context, method, arguments, result);
    }
}
