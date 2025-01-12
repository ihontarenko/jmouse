package svit.proxy;

/**
 * Interface representing a method interceptor in a proxy mechanism.
 * <p>
 * A {@link MethodInterceptor} allows custom behavior to be executed before,
 * after, or around a method invocation on a proxy instance. Interceptors are
 * typically used to implement cross-cutting concerns such as logging, security,
 * or transaction management.
 * </p>
 *
 * <p>Example Usage:</p>
 * <pre>{@code
 * MethodInterceptor loggingInterceptor = invocation -> {
 *     System.out.println("Before method: " + invocation.getMethod().getName());
 *     Object result = invocation.proceed();
 *     System.out.println("After method: " + invocation.getMethod().getName());
 *     return result;
 * };
 * }</pre>
 */
public interface MethodInterceptor {

    /**
     * Intercepts a method invocation on a proxy instance.
     * <p>
     * Implementations can add behavior before or after the invocation,
     * or even replace the method execution entirely.
     * </p>
     *
     * @param invocation the {@link MethodInvocation} object containing details of the method call.
     * @return the result of the method invocation.
     * @throws Throwable if an error occurs during method interception or execution.
     */
    Object invoke(MethodInvocation invocation) throws Throwable;

}
