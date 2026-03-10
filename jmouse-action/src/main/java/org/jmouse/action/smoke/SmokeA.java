package org.jmouse.action.smoke;

import org.jmouse.action.*;
import org.jmouse.action.adapter.el.ActionELModule;
import org.jmouse.action.adapter.el.ActionExpressionAdapter;
import org.jmouse.core.mapping.Mappers;
import org.jmouse.core.scope.AbstractContext;
import org.jmouse.core.scope.BeanProvider;
import org.jmouse.core.scope.Context;
import org.jmouse.el.ExpressionLanguage;

public class SmokeA {


    public static void main(String... arguments) {

        ExpressionLanguage el = new ExpressionLanguage();
        ActionELModule.configure(el);

//        ActionRegistry registry = new DefaultActionRegistry();
//
//        registry.register("autoload", request -> {
//            System.out.println("executing autoload");
//
//            request.arguments().forEach(
//                    (k,v) -> System.out.println(k + " = " + v));
//
//            return null;
//        });

        ActionRegistry registryB = new ConfigurableActionRegistry(new DefaultActionRegistry(), Mappers.defaultMapper())
                .register("autoload", request -> {
                    System.out.println("executing autoload");
                    System.out.println(request.arguments());
                    return null;
                })
                .register("loadEnum", EnumDataLoader.class)
                .unwrap();

        ActionExecutor executor = new DefaultActionExecutor(registryB);

        ActionExpressionAdapter adapter =
                new ActionExpressionAdapter(el, executor);

        Context context = new AbstractContext() {
            @Override
            public void setBeanProvider(BeanProvider beanProvider) {
                super.setBeanProvider(beanProvider);
            }
        };

        adapter.execute(
                "@Action[autoload]{'source':'user'}",
                context
        );

        adapter.execute(
                "@Action[loadEnum]{'source' : 'org.jmouse.action.ActionDefinition' | class, 'target' : class('org.jmouse.action.ActionDefinition')}",
                context
        );
    }

}
