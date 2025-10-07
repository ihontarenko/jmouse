package org.jmouse.security.web.authentication.www;

import org.jmouse.security.web.authentication.AbstractRealmAuthenticationEntryPoint;
import org.jmouse.web.http.request.WWWAuthenticate;

/**
 * 🔑 BasicAuthenticationEntryPoint
 *
 * Sends an HTTP Basic authentication challenge with a configured realm.
 *
 * <p>Responsibilities:</p>
 * <ul>
 *   <li>📢 Builds a {@code WWW-Authenticate: Basic realm="..."} header</li>
 *   <li>🚫 Ensures response is {@code 401 Unauthorized}</li>
 *   <li>🏷️ Realm name is inherited from {@link AbstractRealmAuthenticationEntryPoint}</li>
 * </ul>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * BasicAuthenticationEntryPoint entryPoint = new BasicAuthenticationEntryPoint();
 * entryPoint.setRealmName("App-API");
 * }</pre>
 */
public class BasicAuthenticationEntryPoint extends AbstractRealmAuthenticationEntryPoint {

    /**
     * 🔧 Provides the Basic Auth challenge.
     *
     * @return {@link WWWAuthenticate} with scheme = "Basic" and configured realm
     */
    @Override
    protected WWWAuthenticate getWWWAuthenticate() {
        return WWWAuthenticate.basic().realm(getRealmName());
    }
}
