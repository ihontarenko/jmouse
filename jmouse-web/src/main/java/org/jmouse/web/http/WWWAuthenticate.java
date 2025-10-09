package org.jmouse.web.http;

import org.jmouse.core.Charset;
import org.jmouse.util.StringHelper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * 🔐 Builder for {@code WWW-Authenticate} challenges.
 *
 * <p>Supports common schemes (Basic, Bearer, Digest, Negotiate, NTLM),
 * realm, token68, and arbitrary parameters.</p>
 *
 * <h3>Examples</h3>
 * <pre>{@code
 * // Basic
 * String basic = WWWAuthenticate.basic("MyRealm")
 *     .charset(Charset.UTF_8)
 *     .toHeaderValue();
 * // -> "Basic, realm=\"MyRealm\", charset=UTF-8"
 *
 * // Bearer error
 * String bearer = WWWAuthenticate.bearer()
 *     .error("invalid_token")
 *     .errorDescription("Token expired")
 *     .scope("read write")
 *     .toHeaderValue();
 * // -> "Bearer, error=invalid_token, error_description=Token expired, scope=read write"
 *
 * // Digest (simplified)
 * String digest = WWWAuthenticate.digest("my-realm")
 *     .nonce("abc123")
 *     .algorithm("SHA-256")
 *     .qop("auth")
 *     .toHeaderValue();
 * // -> "Digest, realm=\"my-realm\", nonce=abc123, algorithm=SHA-256, qop=auth"
 *
 * // Negotiate with token68 (base64)
 * String negotiate = WWWAuthenticate.negotiate("TlRMTVNTUAABAAA...")
 *     .toHeaderValue();
 * // -> "Negotiate"
 *
 * // Multiple challenges are typically sent as separate header values by server framework
 * }</pre>
 */
public class WWWAuthenticate extends AbstractHeader {

    /**
     * Standard auth scheme identifiers.
     */
    public static final String BASIC_SCHEME     = "Basic";
    public static final String BEARER_SCHEME    = "Bearer";
    public static final String DIGEST_SCHEME    = "Digest";
    public static final String NEGOTIATE_SCHEME = "Negotiate";
    public static final String NTLM_SCHEME      = "NTLM";

    /**
     * Selected scheme name (e.g., "Basic").
     */
    protected String              scheme;
    /**
     * Optional realm value (quoted in output).
     */
    protected String              realm;
    /**
     * Challenge parameters (key=value pairs as-is).
     */
    protected Map<String, String> parameters;
    /**
     * Optional token68 value (for Negotiate/NTLM, etc.).
     */
    protected String              token68;

    private WWWAuthenticate() {
        super(HttpHeader.WWW_AUTHENTICATE);
        parameters = new LinkedHashMap<>();
    }

    // ---------- Factories ----------

    /**
     * 🧰 Create a Basic challenge builder.
     */
    public static Basic basic() {
        return (Basic) new Basic().scheme(BASIC_SCHEME);
    }

    /**
     * 🧰 Create a Basic challenge with a realm.
     */
    public static Basic basic(String realm) {
        return (Basic) basic().realm(realm);
    }

    /**
     * 🧰 Create a Bearer challenge builder.
     */
    public static Bearer bearer() {
        return (Bearer) new Bearer().scheme(BEARER_SCHEME);
    }

    /**
     * 🧰 Create a Bearer challenge with a realm.
     */
    public static Bearer bearer(String realm) {
        return (Bearer) bearer().realm(realm);
    }

    /**
     * 🧰 Create a Digest challenge builder.
     */
    public static Digest digest() {
        return (Digest) new Digest().scheme(DIGEST_SCHEME);
    }

    /**
     * 🧰 Create a Digest challenge with a realm.
     */
    public static Digest digest(String realm) {
        return (Digest) digest().realm(realm);
    }

    /**
     * 🧰 Create a Negotiate challenge builder.
     */
    public static Negotiate negotiate() {
        return (Negotiate) new Negotiate().scheme(NEGOTIATE_SCHEME);
    }

    /**
     * 🧰 Create a Negotiate challenge with token68.
     */
    public static Negotiate negotiate(String t68) {
        return (Negotiate) negotiate().token68(t68);
    }

    /**
     * 🧰 Create an NTLM challenge builder.
     */
    public static Ntlm ntlm() {
        return (Ntlm) new Ntlm().scheme(NTLM_SCHEME);
    }

    /**
     * 🧰 Create an NTLM challenge with token68.
     */
    public static Ntlm ntlm(String t68) {
        return (Ntlm) ntlm().token68(t68);
    }

    // ---------- Rendering ----------

    /**
     * 🧾 Render the header value.
     *
     * <p>Format: {@code <scheme>, realm="..", key=value, ...}</p>
     * <p><b>Note:</b> Current output does not append {@code token68} to the value.
     * If you need {@code "<scheme> <token68>"} format for Negotiate/NTLM, extend rendering accordingly.</p>
     */
    @Override
    public String toHeaderValue() {
        StringBuilder builder    = new StringBuilder(this.scheme);
        List<String>  directives = new ArrayList<>();

        if (this.token68 != null) {
            builder.append(' ').append(this.token68);
        }

        if (this.realm != null) {
            directives.add("realm=" + StringHelper.quote(this.realm));
        }

        if (!parameters.isEmpty()) {
            parameters.forEach((key, value) -> directives.add(keyValue(key, value, "%s=%s"::formatted)));
        }

        builder.append(' ');

        return builder + String.join(", ", directives);
    }

    /**
     * 🔗 Key-value rendering hook (kept for custom formatting).
     */
    private String keyValue(String key, String value, BiFunction<String, String, String> transformer) {
        return transformer.apply(key, value);
    }

    // ---------- Mutators (fluent) ----------

    /**
     * 🧭 Set auth scheme (e.g. {@code "Basic"}).
     */
    public WWWAuthenticate scheme(String scheme) {
        this.scheme = scheme;
        return this;
    }

    /**
     * 🏰 Set realm (will be quoted).
     */
    public WWWAuthenticate realm(String realm) {
        this.realm = realm;
        return this;
    }

    /**
     * 🎫 Set token68 (e.g. for Negotiate/NTLM).
     */
    public WWWAuthenticate token68(String token68) {
        this.token68 = token68;
        return this;
    }

    /**
     * ➕ Add arbitrary parameter (kept as-is).
     */
    public WWWAuthenticate parameter(String parameter, String value) {
        parameters.put(parameter, value);
        return this;
    }

    // ---------- Scheme specializations ----------

    /**
     * 📄 Basic scheme helper.
     */
    public static final class Basic extends WWWAuthenticate {

        /**
         * 🔤 Set {@code charset} parameter (e.g., {@code UTF-8}).
         */
        public Basic charset(Charset charset) {
            return (Basic) parameter("charset", charset.name());
        }
    }

    /**
     * 🪙 Bearer scheme helper (RFC 6750).
     */
    public static final class Bearer extends WWWAuthenticate {

        public enum ErrorCode {
            INVALID_TOKEN, INSUFFICIENT_SCOPE, INVALID_REQUEST
        }

        /**
         * 🎯 Set {@code scope} parameter (space-delimited).
         */
        public Bearer scope(String scope) {
            return (Bearer) parameter("scope", scope);
        }

        /**
         * ❗ Set {@code error} parameter.
         * <p>Common values: {@code invalid_request}, {@code invalid_token}, {@code insufficient_scope}.</p>
         */
        public Bearer error(ErrorCode errorCode) {
            return (Bearer) parameter("error", errorCode.name().toLowerCase());
        }

        /**
         * Shortcut: {@code error=invalid_request}.
         */
        public Bearer invalidRequest() {
            return error(ErrorCode.INVALID_REQUEST);
        }

        /**
         * Shortcut: {@code error=invalid_token}.
         */
        public Bearer invalidToken() {
            return error(ErrorCode.INVALID_TOKEN);
        }

        /**
         * Shortcut: {@code error=insufficient_scope}.
         */
        public Bearer insufficientScope() {
            return error(ErrorCode.INSUFFICIENT_SCOPE);
        }

        /**
         * 📝 Set {@code error_description}.
         */
        public Bearer errorDescription(String desc) {
            return (Bearer) parameter("error_description", desc);
        }

        /**
         * 🔗 Set {@code error_uri}.
         */
        public Bearer errorUri(String uri) {
            return (Bearer) parameter("error_uri", uri);
        }
    }

    /**
     * 🤝 Negotiate (SPNEGO/Kerberos) helper.
     * <p>Use {@link #token68(String)} if you need to carry a token.</p>
     */
    public static final class Negotiate extends WWWAuthenticate {
    }

    /**
     * 🧮 Digest scheme helper.
     */
    public static final class Digest extends WWWAuthenticate {

        /**
         * Set {@code domain}.
         */
        public Digest domain(String domain) {
            return (Digest) parameter("domain", domain);
        }

        /**
         * Set {@code nonce}.
         */
        public Digest nonce(String nonce) {
            return (Digest) parameter("nonce", nonce);
        }

        /**
         * Set {@code opaque}.
         */
        public Digest opaque(String opaque) {
            return (Digest) parameter("opaque", opaque);
        }

        /**
         * Set {@code stale}.
         */
        public Digest stale(boolean stale) {
            return (Digest) parameter("stale", String.valueOf(stale));
        }

        /**
         * Set {@code algorithm}.
         */
        public Digest algorithm(String algorithm) {
            return (Digest) parameter("algorithm", algorithm);
        }

        /**
         * Shortcut: {@code algorithm=MD5}.
         */
        public Digest md5() {
            return algorithm("MD5");
        }

        /**
         * Shortcut: {@code algorithm=SHA-256}.
         */
        public Digest sha256() {
            return algorithm("SHA-256");
        }

        /**
         * Shortcut: {@code algorithm=SHA-512}.
         */
        public Digest sha512() {
            return algorithm("SHA-512");
        }

        /**
         * Set {@code qop} (e.g., {@code auth}, {@code auth-int}).
         */
        public Digest qop(String qop) {
            return (Digest) parameter("qop", qop);
        }

        /**
         * Set {@code userhash}.
         */
        public Digest userHash(boolean userhash) {
            return (Digest) parameter("userhash", String.valueOf(userhash));
        }

        /**
         * Convenience: join multiple domains with space.
         */
        public Digest domain(String... domains) {
            return (domains == null) ? this : domain(String.join(" ", domains));
        }
    }

    /**
     * 🧱 NTLM helper.
     * <p>Use {@link #token68(String)} to attach base64 token if necessary.</p>
     */
    public static final class Ntlm extends WWWAuthenticate {
    }
}
