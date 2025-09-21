package org.jmouse.core.proxy2.interceptors;

import org.jmouse.core.proxy2.api.Advisor;
import org.jmouse.core.proxy2.api.InvocationPipeline;
import org.jmouse.core.proxy2.api.MethodInterceptor;
import org.jmouse.core.proxy2.api.MethodInvocation;

import java.util.ArrayList;
import java.util.List;

public class AdvisorApplyingInterceptor implements MethodInterceptor {

    private final List<Advisor> advisors;

    AdvisorApplyingInterceptor(List<Advisor> advisors) {
        this.advisors = advisors;
    }

    @Override
    public Object invoke(MethodInvocation inv) throws Throwable {
        List<MethodInterceptor> chain = new ArrayList<>();

        for (var a : advisors) {
            if (a.pointcut().classMatches(inv.getTarget().getClass())
                    && a.pointcut().methodMatcher().matches(inv.getMethod())) {
                chain.add(a.interceptor());
            }
        }

        InvocationPipeline.Terminal terminal = MethodInvocation::proceed;
        MethodInterceptor           pipeline = InvocationPipeline.assemble(chain, terminal);

        return pipeline.invoke(inv);
    }

}
