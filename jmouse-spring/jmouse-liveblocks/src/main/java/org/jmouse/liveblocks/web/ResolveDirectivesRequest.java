package org.jmouse.liveblocks.web;

import org.jmouse.liveblocks.Directive;

import java.util.List;

/**
 * Every directive one document contains, in one request.
 *
 * <p>⚠️ <strong>A batch, and it has to be.</strong> A page with twenty {@code :::issue} lines would
 * otherwise be twenty cross-origin round trips, each one a token validation and a query, on a path that
 * runs every time somebody opens the page.
 *
 * @param directives what the document asks. ⚠️ Capped by {@link DirectiveResolveController}: no real
 *                   page carries a hundred live blocks, and a request claiming to is either a mistake or
 *                   somebody probing the endpoint
 */
public record ResolveDirectivesRequest(List<Directive> directives) {

    public List<Directive> asked() {
        return directives == null ? List.of() : directives;
    }

}
