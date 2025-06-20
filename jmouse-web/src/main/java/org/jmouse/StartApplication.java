package org.jmouse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jmouse.beans.annotation.Factories;
import org.jmouse.beans.annotation.Provide;
import org.jmouse.context.ApplicationBeanContext;
import org.jmouse.core.proxy.MethodInterceptor;
import org.jmouse.core.proxy.MethodInvocation;
import org.jmouse.core.proxy.ProxyContext;
import org.jmouse.core.proxy.annotation.ProxyMethodInterceptor;
import org.jmouse.mvc.WebApplicationLauncher;
import org.jmouse.mvc.WebMvcInitializer;
import org.jmouse.mvc.mapping.DirectRequestPathMapping.Registration;
import org.jmouse.web.server.WebServerFactory;

import java.io.IOException;
import java.lang.reflect.Method;

public class StartApplication {

    public static void main(String... arguments) {
        new WebApplicationLauncher(StartApplication.class, ApplicationBeanContext.class).launch();
    }

    @Factories
    public static class AppFactories {

        @Provide
        public Registration helloPage() {
            return new Registration("/hello", (request, response) -> response.getWriter().write("Hello World!"));
        }

        @Provide
        public Registration worldPage() {
            return new Registration("/world", (request, response) -> response.getWriter().write("Hello World!!!"));
        }

        @Provide
        public Registration defaultPage() {
            return new Registration("/page", this::page);
        }

        public void page(HttpServletRequest request, HttpServletResponse response) throws IOException {
            response.setContentType("text/html");
            response.setStatus(HttpServletResponse.SC_ACCEPTED);
            response.getWriter().write("<h1>org.jmouse.StartApplication.AppFactories.page</h1>");
        }

    }

    public static class AppInitializer implements WebMvcInitializer<Object> {

        @Override
        public void initialize(Object object) {
            System.out.println("Initializing " + object.getClass().getName());
        }

        @Override
        public Class<Object> objectClass() {
            return Object.class;
        }
    }

    @ProxyMethodInterceptor(WebServerFactory.class)
    public static class WebServerFactoryInterceptor implements MethodInterceptor {

        @Override
        public void before(ProxyContext context, Method method, Object[] arguments) {
            System.out.println("before");
            System.out.println(method.getName());
        }

        @Override
        public Object invoke(MethodInvocation invocation) throws Throwable {
            System.out.println("invoke");
            System.out.println(invocation.getTarget());
            return invocation.proceed();
        }

        @Override
        public void after(ProxyContext context, Method method, Object[] arguments, Object result) {
            System.out.println("after");
            System.out.println(method.getName());
        }
    }

}
