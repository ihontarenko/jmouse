package org.jmouse.security.core.access;

import org.jmouse.el.evaluation.EvaluationContext;

public interface MethodExpressionHandler<T> extends ExpressionHandler<T> {

    String VARIABLE_ARGUMENTS    = "arguments";
    String VARIABLE_RETURN_VALUE = "returnValue";

    default void setReturnValue(Object result, EvaluationContext context) {
        context.setValue(VARIABLE_RETURN_VALUE, result);
    }

    default RoleHierarchy getRoleHierarchy() {
        return RoleHierarchy.none();
    }

    default PermissionEvaluator getPermissionEvaluator() {
        return new PermissionEvaluator.DenyAll();
    }

}
