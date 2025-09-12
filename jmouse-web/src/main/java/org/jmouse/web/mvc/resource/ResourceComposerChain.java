package org.jmouse.web.mvc.resource;

import org.jmouse.core.chain.Chain;

public interface ResourceComposerChain extends Chain<String, UrlComposerContext, String> {

    default String compose(String relative, UrlComposerContext context) {
        return run(relative, context);
    }

}
