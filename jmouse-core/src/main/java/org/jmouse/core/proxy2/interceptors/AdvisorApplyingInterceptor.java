package org.jmouse.core.proxy2.interceptors;

import org.jmouse.core.proxy2.aop.AdvisorChainFactory;
import org.jmouse.core.proxy2.aop.RuntimePointcut;
import org.jmouse.core.proxy2.api.InvocationPipeline;
import org.jmouse.core.proxy2.api.MethodInterceptor;
import org.jmouse.core.proxy2.api.MethodInvocation;

import java.lang.reflect.Method;

public class AdvisorApplyingInterceptor implements MethodInterceptor {

    private final AdvisorChainFactory chains;

    public AdvisorApplyingInterceptor(AdvisorChainFactory factory) {
        this.chains = factory;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Method   method     = invocation.getMethod();
        Class<?> targetType = invocation.getTarget() != null ? invocation.getTarget().getClass() : invocation.getProxy().getClass();

        AdvisorChainFactory.CompiledChain chain = chains.chainFor(targetType, method);

        // runtime guard: if any EL/guard present — verify
        for (RuntimePointcut runtimePointcut : chain.pointcuts()) {
            if (!runtimePointcut.runtimeAccept(invocation.getProxy(), invocation.getTarget(), method, invocation.getArguments())) {
                // guard failed -> skip advisors fully
                return invocation.proceed();
            }
        }

        // assemble inner pipeline (no terminal—return to outer cursor via proceed())
        var inner = InvocationPipeline.assemble(chain.interceptors(), invocation1 -> );
        return inner.invoke(invocation);
    }
}
