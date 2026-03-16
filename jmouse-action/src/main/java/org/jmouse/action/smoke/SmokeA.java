package org.jmouse.action.smoke;

import org.jmouse.action.*;
import org.jmouse.action.adapter.el.ActionExpressionAdapter;
import org.jmouse.action.annotation.Action;
import org.jmouse.core.scope.Context;

import java.util.Map;

public class SmokeA {


    public static void main(String... arguments) {

        ActionExecutor executor = ActionExecutorSingleton.getActionExecutor(SmokeA.class);
        ActionExpressionAdapter adapter = ActionExecutorSingleton.getActionExpressionAdapter(executor);

        Context context = new ActionExecutionContext();

        executor.execute(ActionDefinition.create("default:autoload", Map.of("a", "b")), context);

        adapter.execute(
                "@[default:autoload]{'source':'user'}",
                context
        );

        adapter.execute(
                "@[default:autoloadB]{'source':'A', 'target':22d/7}",
                context
        );

        adapter.execute(
                "@[default:autoloadB]{'source':'B','target':[1, 2, 4, {'a':33D/9}]|list}",
                context
        );
//
//        adapter.execute(
//                "@Action[loadEnum]{'source' : 'org.jmouse.action.ActionDefinition' | class, 'target' : class('org.jmouse.action.ActionDefinition')}",
//                context
//        );
    }

    @Action("default:autoloadB")
    public void autoload(ActionRequest request, SourceTarget sourceTarget) {
        System.out.println("B");
    }

}
