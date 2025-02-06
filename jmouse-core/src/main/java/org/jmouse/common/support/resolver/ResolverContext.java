package org.jmouse.common.support.resolver;

import org.jmouse.common.support.context.AbstractVariablesContext;
import org.jmouse.common.support.context.Context;

public class ResolverContext extends AbstractVariablesContext {

    public ResolverContext(Context context) {
        copyFrom(context);
    }

}
