package org.jmouse.web.negotiation;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.beans.InitializingBeanSupport;
import org.jmouse.core.MediaType;
import org.jmouse.web.context.WebBeanContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 🧩 Media type manager (resolver chain).
 *
 * <p>Aggregates {@link MediaTypeLookup} instances and determines the media types
 * requested by the client from an {@link HttpServletRequest} using a
 * <em>first non-empty wins</em> strategy: the first lookup that yields a
 * non-empty result stops the search.</p>
 *
 * <p>Used during content negotiation as a source of candidate media types
 * for further selection.</p>
 *
 * ⚠️ The order of registered lookups defines their priority.
 *
 * @author Ivan Hontarenko
 * @see MediaTypeLookup
 * @see LookupRegistry
 */
public class MediaTypeManager implements LookupRegistry, InitializingBeanSupport<WebBeanContext> {

    /** 🔗 Registered lookups; iteration order defines priority. */
    private final List<MediaTypeLookup> strategies = new ArrayList<>();

    /**
     * 📚 Returns the current list of lookups in invocation order.
     *
     * @return the registered {@link MediaTypeLookup} instances
     */
    @Override
    public List<MediaTypeLookup> getLookups() {
        return strategies;
    }

    /**
     * ➕ Registers a new lookup at the end of the chain.
     *
     * @param lookup the media type lookup to register
     */
    @Override
    public void registerLookup(MediaTypeLookup lookup) {
        strategies.add(lookup);
    }

    /**
     * 🔍 Resolves requested media types from the given request.
     *
     * <p>Iterates over the registered lookups and returns the first non-empty
     * result. If no lookup produces candidates, an empty collection is returned.</p>
     *
     * @param request the HTTP request
     * @return a collection of media types; may be empty but never {@code null}
     */
    public Collection<MediaType> lookupOnRequest(HttpServletRequest request) {
        Collection<MediaType> types = new ArrayList<>();

        for (MediaTypeLookup lookup : getLookups()) {
            Collection<MediaType> candidates = lookup.lookup(request);
            if (candidates != null && !candidates.isEmpty()) {
                types = candidates;
                break;
            }
        }

        return types;
    }

    /**
     * ⚙️ Auto-registers lookups from the application context.
     *
     * <p>On initialization, fetches all {@link MediaTypeLookup} beans from the
     * provided {@link WebBeanContext} and registers them in the chain.</p>
     *
     * @param context the web application context
     */
    @Override
    public void doInitialize(WebBeanContext context) {
        for (MediaTypeLookup mediaTypeLookup : context.getBeans(MediaTypeLookup.class)) {
            registerLookup(mediaTypeLookup);
        }
    }
}
