package org.jmouse.context.proxy.interceptors;

import org.jmouse.context.proxy.aop.AdvisorChainFactory;
import org.jmouse.context.proxy.aop.Pointcut;
import org.jmouse.context.proxy.aop.RuntimeMatcher;
import org.jmouse.context.proxy.api.InvocationPipeline;
import org.jmouse.context.proxy.api.MethodInterceptor;
import org.jmouse.context.proxy.api.MethodInvocation;

import java.lang.reflect.Method;

public class AdvisorApplyingInterceptor implements MethodInterceptor {

    private final AdvisorChainFactory factory;

    public AdvisorApplyingInterceptor(AdvisorChainFactory factory) {
        this.factory = factory;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method   method     = invocation.getMethod();
        Class<?> targetType = invocation.getTarget() != null ? invocation.getTarget().getClass() : invocation.getProxy().getClass();

        AdvisorChainFactory.Compiled chain = factory.chainFor(targetType, method);

        for (Pointcut pointcut : chain.pointcuts()) {
            boolean unacceptable = true;

            if (pointcut instanceof RuntimeMatcher predicate) {
                unacceptable = !predicate.runtimeMatches(
                        invocation.getProxy(), invocation.getTarget(), method, invocation.getArguments());
            }

            if (unacceptable) {
                return invocation.proceed();
            }
        }

        // assemble inner pipeline (no terminal—return to outer cursor via proceed())
        return InvocationPipeline.assemble(chain.interceptors(), MethodInvocation::proceed).invoke(invocation);
    }
}
