package svit.support.resolver;

import svit.support.context.AbstractVariablesContext;
import svit.support.context.Context;

public class ResolverContext extends AbstractVariablesContext {

    public ResolverContext(Context context) {
        copyFrom(context);
    }

}
