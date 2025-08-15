package org.jmouse.web.negotiation;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.MediaType;

import java.util.List;

/**
 * 🔍 Strategy for extracting client-requested media types from an HTTP request.
 *
 * <p>Implementations read a single source of preference (e.g. {@code Accept} header,
 * path extension, query parameter, request attribute) and return zero or more
 * candidate {@link MediaType}s.</p>
 *
 * <h4>Contract</h4>
 * <ul>
 *   <li>Never return {@code null}; return an empty list if no candidates are found.</li>
 *   <li>Preserve client preference order when applicable (e.g., by q-factors).</li>
 *   <li>Must be thread-safe and side-effect free.</li>
 *   <li>Be lenient to malformed input; prefer returning an empty list over throwing.</li>
 * </ul>
 *
 * @author Ivan Hontarenko
 */
public interface MediaTypeLookup {

    /**
     * Resolve candidate media types requested by the client from the given request.
     *
     * @param request the current HTTP request
     * @return an ordered list of candidate media types; may be empty but never {@code null}
     */
    List<MediaType> lookup(HttpServletRequest request);

}
