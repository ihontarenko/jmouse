package org.jmouse.security.web.context;

import org.jmouse.security.core.SecurityContext;
import org.jmouse.web.http.request.RequestContextKeeper;

/**
 * 🗄️ SecurityContextRepository
 *
 * Abstraction for persisting and retrieving a {@link SecurityContext} across web requests.
 * Implementations may use HTTP session, cookies, headers, or stateless tokens.
 *
 * <p>Typical responsibilities:</p>
 * <ul>
 *   <li>📥 Load the current {@link SecurityContext} for an incoming request</li>
 *   <li>💾 Save (persist/update) the context after authentication/authorization</li>
 *   <li>🧹 Clear the context on logout or session invalidation</li>
 *   <li>🔎 Check if a context is already present for the request</li>
 * </ul>
 */
public interface SecurityContextRepository {

    /**
     * 📥 Load the {@link SecurityContext} for the current request.
     *
     * @param requestContext holder providing access to request/response
     * @return the resolved {@link SecurityContext}; may be an empty context if none is found
     */
    SecurityContext load(RequestContextKeeper requestContext);

    /**
     * 💾 Persist or update the {@link SecurityContext} for this request/response.
     *
     * <p>Called after successful authentication or context mutation.</p>
     *
     * @param context        context to save
     * @param requestContext holder providing access to request/response
     */
    void save(SecurityContext context, RequestContextKeeper requestContext);

    /**
     * 🧹 Remove any stored {@link SecurityContext} (e.g., on logout).
     *
     * @param context        context to clear (may be ignored by some impls)
     * @param requestContext holder providing access to request/response
     */
    void clear(SecurityContext context, RequestContextKeeper requestContext);

    /**
     * 🔎 Check whether a {@link SecurityContext} is already associated with this request.
     *
     * @param context        candidate context (optional, may be {@code null} in some impls)
     * @param requestContext holder providing access to request/response
     * @return {@code true} if a context is present and considered valid; {@code false} otherwise
     */
    boolean contains(SecurityContext context, RequestContextKeeper requestContext);
}
