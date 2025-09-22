package org.jmouse.context.proxy;

import org.jmouse.context.proxy.aop.AdvisorApplyingInterceptor;
import org.jmouse.context.proxy.aop.AdvisorChainFactory;
import org.jmouse.context.proxy.aop.ProxyAdvisorRegistry;
import org.jmouse.context.proxy.api.*;
import org.jmouse.context.proxy.app.ServicePointcuts;
import org.jmouse.context.proxy.runtime.ByteBuddyProxyEngine;
import org.jmouse.context.proxy.runtime.JdkProxyEngine;
import org.jmouse.core.proxy.ProxyHelper;
import org.jmouse.el.ExpressionLanguage;

import java.lang.reflect.Method;

public class Smoke {

    public static class ReportService {
        public String generate(int id) { return "R#" + id; }
    }

    public static class Logging implements MethodInterceptor {

        @Override
        public void before(InvocationContext context, Method method, Object[] arguments) {
            System.out.println("Before " + method.getName());
        }

        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            System.out.println("Invocation!");
            return invocation.proceed();
        }

        @Override
        public void after(InvocationContext context, Method method, Object[] arguments, Object result) {
            System.out.println("After " + method.getName());
        }
    }

    public static void main(String[] args) throws NoSuchMethodException {
        ExpressionLanguage   el       = new ExpressionLanguage();
        ProxyAdvisorRegistry registry = new ProxyAdvisorRegistry(el);

        registry.scan(ServicePointcuts.class);

        AdvisorChainFactory chains          = new AdvisorChainFactory(registry);
        MethodInterceptor   advisorsApplier = new AdvisorApplyingInterceptor(chains);

        ProxyDefinition<ReportService> def = ProxyDefinition.builder(ReportService.class)
                .classLoader(Smoke.class.getClassLoader())
                .instanceProvider(InstanceProvider.singleton(new ReportService()))
                .intercept(advisorsApplier)
                .intercept(new Logging())
                .policy(InterceptionPolicy.defaultPolicy())
                .build();

        ReportService proxy = new ProxyFactory()
                .register(new JdkProxyEngine())
                .register(new ByteBuddyProxyEngine())
                .create(def);

        System.out.println(
                ProxyHelper.invoke(proxy, ReportService.class.getMethod("generate", int.class), new Object[]{222})
        );

        System.out.println(proxy.generate(1));

    }

}
