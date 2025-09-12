package org.jmouse.web.mvc.resource;

import org.jmouse.core.chain.Link;
import org.jmouse.core.chain.Outcome;

public interface ResourceComposer extends Link<String, UrlComposerContext, String> {

    default Outcome<String> compose(
            String relative,
            UrlComposerContext context,
            ResourceComposerChain next
    ) {
        return handle(relative, context, next);
    }

}
