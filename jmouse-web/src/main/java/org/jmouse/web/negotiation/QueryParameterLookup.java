package org.jmouse.web.negotiation;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 🔎 Query-parameter based media type resolver.
 *
 * <p>Concrete {@link MappedMediaTypeLookup} that derives the mapping key from a
 * request query parameter (default: {@code "format"}). Typical values are
 * {@code json}, {@code xml}, {@code html}, etc. The resolved key is then mapped
 * to a {@code MediaType} via {@link #addExtension(String, org.jmouse.core.MediaType)}.</p>
 *
 * <h4>Notes</h4>
 * <ul>
 *   <li>Key is read via {@link HttpServletRequest#getParameter(String)} (first value wins).</li>
 *   <li>Key may be {@code null}; the base class will return an empty list in that case.</li>
 *   <li>Case sensitivity depends on how keys are registered (e.g., register lowercase and
 *       normalize input if needed).</li>
 *   <li>Prefer configuring {@link #setKeyName(String)} (String)} during initialization and
 *       avoiding runtime mutations for thread-safety.</li>
 * </ul>
 *
 * <h4>Example</h4>
 * <pre>{@code
 * QueryParameterLookup lookup = new QueryParameterLookup("format");
 * lookup.addExtension("json", MediaType.APPLICATION_JSON);
 * lookup.addExtension("xml",  MediaType.APPLICATION_XML);
 * // GET /api/items?format=json  -> resolves application/json
 * }</pre>
 *
 * @author Ivan Hontarenko
 * @see MappedMediaTypeLookup
 */
public class QueryParameterLookup extends MappedMediaTypeLookup {

    /** Creates a lookup using the default parameter name {@code "format"}. */
    public QueryParameterLookup() {
        super(DEFAULT_FORMAT_ATTRIBUTE);
    }

    /**
     * Extracts the mapping key from the current request's query parameter.
     *
     * @param request the current HTTP request
     * @return the raw parameter value (may be {@code null})
     */
    @Override
    protected String getMappingKey(HttpServletRequest request) {
        return request.getParameter(getKeyName());
    }
}
