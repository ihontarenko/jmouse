package org.jmouse.web.mvc.resource;

import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Outcome;

/**
 * 🚫 No-op implementation of {@link ResourceComposer}.
 *
 * <p>Acts as a neutral placeholder that performs no composition
 * and simply delegates to the next element in the chain.</p>
 *
 * <p>💡 Useful as a default when no special resource
 * composition logic is required.</p>
 */
public enum NoopComposer implements ResourceComposer {
    /**
     * 🧩 Singleton instance.
     */
    INSTANCE;

    /**
     * ▶️ Always skip to the next composer in the chain.
     *
     * @param relative resolved resource path {@link String}
     * @param context    active {@link UrlComposerContext}
     * @param next     downstream {@link Chain} element
     * @return {@link Outcome#next()} to continue without modification
     */
    @Override
    public Outcome<String> handle(
            String relative, UrlComposerContext context, Chain<String, UrlComposerContext, String> next) {
        return Outcome.next();
    }
}
