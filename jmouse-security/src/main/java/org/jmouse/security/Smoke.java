package org.jmouse.security;

import org.jmouse.core.proxy.DefaultProxyFactory;
import org.jmouse.core.proxy.ProxyFactory;
import org.jmouse.security.authentication.AnonymousAuthentication;
import org.jmouse.security.authorization.method.AuthorizeMethodInterceptor;
import org.jmouse.security.authorization.method.AuthorizeMethodManager;
import org.jmouse.security.core.access.annotation.Authorize;

public class Smoke {

    public static void main(String... arguments) {
        SecurityContextHolder.getContext().setAuthentication(new AnonymousAuthentication("ANONYMOUS"));

        ProxyFactory factory = new DefaultProxyFactory(
                new AuthorizeMethodInterceptor(new AuthorizeMethodManager())
        );

        Service service = new Service();

        service.dummy("direct");

        Service     proxy       = factory.createProxy(service);
        User        user        = factory.createProxy(new DefaultUser());
        UserService userService = factory.createProxy(new UserService());

        proxy.dummy("proxied");

        System.out.println("stop");

        proxy.dummy("proxied again");

        System.out.println(userService.upper("Ivan"));

        System.out.println("end!");

    }

    public static class Service {

        @Authorize("(a:authentication, t:'R_ADMIN') -> arguments[0] == a.principal.name")
        public void dummy(String name) {
            System.out.println("dummy call: " + name);
        }

    }

    interface User {

        String getName();

    }

    public static class DefaultUser implements User {

        @Override
        public String getName() {
            return "name";
        }
    }

}
