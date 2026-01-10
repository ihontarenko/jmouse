package org.jmouse.common.support.resolver;

import org.jmouse.core.scope.AbstractVariablesContext;
import org.jmouse.core.scope.Context;

public class ResolverContext extends AbstractVariablesContext {

    public ResolverContext(Context context) {
        copyFrom(context);
    }

}
