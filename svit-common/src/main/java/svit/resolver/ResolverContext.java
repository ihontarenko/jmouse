package svit.resolver;

import svit.context.AbstractVariablesContext;
import svit.context.Context;

public class ResolverContext extends AbstractVariablesContext {

    public ResolverContext(Context context) {
        copyFrom(context);
    }

}
