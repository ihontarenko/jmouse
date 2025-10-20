package org.jmouse.security.jwt;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 📦 Immutable representation of a JSON Web Token (JWT).
 *
 * <p>Contains:</p>
 * <ul>
 *   <li>🔑 The raw token value</li>
 *   <li>📜 Header parameters</li>
 *   <li>🧾 Claims (payload)</li>
 *   <li>⏱️ Standard timing fields: {@link #issuedAt}, {@link #expiresAt}</li>
 *   <li>🙍 Subject identifier (sub claim)</li>
 * </ul>
 */
public record Jwt(
        String tokenValue,
        Map<String, Object> headers,
        Map<String, Object> claims,
        Instant issuedAt,
        Instant expiresAt,
        String subject
) {

    public static Jwt.Builder builder() {
        return new Jwt.Builder();
    }

    /**
     * 🔍 Retrieve a claim by name with generic typing.
     *
     * @param name claim name
     * @param <T>  expected claim type
     * @return claim value or {@code null} if not present
     */
    @SuppressWarnings("unchecked")
    public <T> T claim(String name) {
        return (T) claims.get(name);
    }

    /**
     * 🔐 Meaning: JWS/JWE algorithm identifier used to secure the JWT.
     * <br>📜 RFC / Spec: <a href="https://www.rfc-editor.org/rfc/rfc7515#section-4.1.1">RFC 7515 §4.1.1</a>
     * <br>🧩 Type: String
     * <br>🧪 Example:
     * <pre>{@code
     * "HS256", "RS256", "ES256", "none"
     * }</pre>
     */
    public static final String HEADER_ALGORITHM = "alg";

    /**
     * 🏷️ Meaning: Media type (type) of the entire JWS/JWE object.
     * <br>📜 RFC / Spec: <a href="https://www.rfc-editor.org/rfc/rfc7519#section-5.1">RFC 7519 §5.1</a>, <a href="https://www.rfc-editor.org/rfc/rfc7515#section-4.1.9">RFC 7515 §4.1.9</a>
     * <br>🧩 Type: String
     * <br>🧪 Example:
     * <pre>{@code
     * "JWT"
     * }</pre>
     */
    public static final String HEADER_TYPE = "typ";

    /**
     * 🗂️ Meaning: Content type of the secured payload (often for nested JWTs).
     * <br>📜 RFC / Spec: <a href="https://www.rfc-editor.org/rfc/rfc7515#section-4.1.10">RFC 7515 §4.1.10</a>
     * <br>🧩 Type: String
     * <br>🧪 Example:
     * <pre>{@code
     * "JWT", "application/json"
     * }</pre>
     */
    public static final String HEADER_CONTENT_TYPE = "cty";

    /**
     * 🔑 Meaning: Key identifier used to match a key in a JWKS.
     * <br>📜 RFC / Spec: <a href="https://www.rfc-editor.org/rfc/rfc7515#section-4.1.4">RFC 7515 §4.1.4</a>
     * <br>🧩 Type: String
     * <br>🧪 Example:
     * <pre>{@code
     * "auth-key-1"
     * }</pre>
     */
    public static final String HEADER_KEY_ID = "kid";

    /**
     * 🌐 Meaning: URL for a JSON Web Key Set containing the public keys.
     * <br>📜 RFC / Spec: <a href="https://www.rfc-editor.org/rfc/rfc7515#section-4.1.2">RFC 7515 §4.1.2</a>
     * <br>🧩 Type: String (URL)
     * <br>🧪 Example:
     * <pre>{@code
     * "https://idp.example.com/.well-known/jwks.json"
     * }</pre>
     */
    public static final String HEADER_JWK_SET_URL = "jku";

    /**
     * 🔏 Meaning: JSON Web Key (public key) embedded directly in the header.
     * <br>📜 RFC / Spec: <a href="https://www.rfc-editor.org/rfc/rfc7515#section-4.1.3">RFC 7515 §4.1.3</a>, <a href="https://www.rfc-editor.org/rfc/rfc7517">RFC 7517</a>
     * <br>🧩 Type: JSON object
     * <br>🧪 Example:
     * <pre>{@code
     * { "kty":"RSA", "n":"...", "e":"AQAB" }
     * }</pre>
     */
    public static final String HEADER_JSON_WEB_KEY = "jwk";

    /**
     * 📜 Meaning: URL to an X.509 public key certificate or certificate chain.
     * <br>📜 RFC / Spec: <a href="https://www.rfc-editor.org/rfc/rfc7515#section-4.1.5">RFC 7515 §4.1.5</a>
     * <br>🧩 Type: String (URL)
     * <br>🧪 Example:
     * <pre>{@code
     * "https://idp.example.com/certs.pem"
     * }</pre>
     */
    public static final String HEADER_X509_URL = "x5u";

    /**
     * 🪪 Meaning: X.509 certificate chain (base64-encoded DER).
     * <br>📜 RFC / Spec: <a href="https://www.rfc-editor.org/rfc/rfc7515#section-4.1.6">RFC 7515 §4.1.6</a>
     * <br>🧩 Type: Array of String
     * <br>🧪 Example:
     * <pre>{@code
     * ["MIID...","MIIC..."]
     * }</pre>
     */
    public static final String HEADER_X509_CERTIFICATE_CHAIN = "x5c";

    /**
     * 🧮 Meaning: X.509 certificate SHA-1 thumbprint.
     * <br>📜 RFC / Spec: <a href="https://www.rfc-editor.org/rfc/rfc7515#section-4.1.7">RFC 7515 §4.1.7</a>
     * <br>🧩 Type: String (base64url)
     * <br>🧪 Example:
     * <pre>{@code
     * "W6ph5Mm5Pz8GgiULbPgzG37mj9g"
     * }</pre>
     */
    public static final String HEADER_X509_CERTIFICATE_SHA1_THUMBPRINT = "x5t";

    /**
     * 🧮 Meaning: X.509 certificate SHA-256 thumbprint (preferred).
     * <br>📜 RFC / Spec: <a href="https://www.rfc-editor.org/rfc/rfc7515#section-4.1.8">RFC 7515 §4.1.8</a>
     * <br>🧩 Type: String (base64url)
     * <br>🧪 Example:
     * <pre>{@code
     * "f0X5... (SHA-256)"
     * }</pre>
     */
    public static final String HEADER_X509_CERTIFICATE_SHA256_THUMBPRINT = "x5t#S256";

    /**
     * 📎 Meaning: List of header parameter names that are critical to understanding the JWT.
     * <br>📜 RFC / Spec: <a href="https://www.rfc-editor.org/rfc/rfc7515#section-4.1.11">RFC 7515 §4.1.11</a>
     * <br>🧩 Type: Array of String
     * <br>🧪 Example:
     * <pre>{@code
     * ["b64","crit"]
     * }</pre>
     */
    public static final String HEADER_CRITICAL = "crit";

    /**
     * 🔐 Meaning: Content encryption algorithm (for JWE).
     * <br>📜 RFC / Spec: <a href="https://www.rfc-editor.org/rfc/rfc7516#section-4.1.2">RFC 7516 §4.1.2</a>
     * <br>🧩 Type: String
     * <br>🧪 Example:
     * <pre>{@code
     * "A256GCM"
     * }</pre>
     */
    public static final String HEADER_ENCRYPTION_ALGORITHM = "enc";

    /**
     * 🗜️ Meaning: Compression algorithm applied to the plaintext before encryption (JWE).
     * <br>📜 RFC / Spec: <a href="https://www.rfc-editor.org/rfc/rfc7516#section-4.1.3">RFC 7516 §4.1.3</a>
     * <br>🧩 Type: String
     * <br>🧪 Example:
     * <pre>{@code
     * "DEF"
     * }</pre>
     */
    public static final String HEADER_COMPRESSION_ALGORITHM = "zip";

    // =============================================================================================
    // 🧾 STANDARD REGISTERED CLAIM KEYS (RFC 7519)
    // =============================================================================================

    /**
     * 🏢 Meaning: Issuer — identifies principal that issued the JWT.
     * <br>📜 RFC / Spec: <a href="https://www.rfc-editor.org/rfc/rfc7519#section-4.1.1">RFC 7519 §4.1.1</a>
     * <br>🧩 Type: String
     * <br>🧪 Example:
     * <pre>{@code
     * "https://auth.example.com"
     * }</pre>
     */
    public static final String CLAIM_ISSUER = "iss";

    /**
     * 👤 Meaning: Subject — identifies principal that is the subject of the JWT.
     * <br>📜 RFC / Spec: <a href="https://www.rfc-editor.org/rfc/rfc7519#section-4.1.2">RFC 7519 §4.1.2</a>
     * <br>🧩 Type: String
     * <br>🧪 Example:
     * <pre>{@code
     * "user123"
     * }</pre>
     */
    public static final String CLAIM_SUBJECT = "sub";

    /**
     * 🎯 Meaning: Audience — recipients for whom the JWT is intended.
     * <br>📜 RFC / Spec: <a href="https://www.rfc-editor.org/rfc/rfc7519#section-4.1.3">RFC 7519 §4.1.3</a>
     * <br>🧩 Type: String or Array of String
     * <br>🧪 Example:
     * <pre>{@code
     * ["api","dashboard"]
     * }</pre>
     */
    public static final String CLAIM_AUDIENCE = "aud";

    /**
     * ⌛ Meaning: Expiration time (seconds since epoch).
     * <br>📜 RFC / Spec: <a href="https://www.rfc-editor.org/rfc/rfc7519#section-4.1.4">RFC 7519 §4.1.4</a>
     * <br>🧩 Type: NumericDate (long)
     * <br>🧪 Example:
     * <pre>{@code
     * 1730000400
     * }</pre>
     */
    public static final String CLAIM_EXPIRATION_TIME = "exp";

    /**
     * 🚫 Meaning: Not before (seconds since epoch).
     * <br>📜 RFC / Spec: <a href="https://www.rfc-editor.org/rfc/rfc7519#section-4.1.5">RFC 7519 §4.1.5</a>
     * <br>🧩 Type: NumericDate (long)
     * <br>🧪 Example:
     * <pre>{@code
     * 1729996800
     * }</pre>
     */
    public static final String CLAIM_NOT_BEFORE = "nbf";

    /**
     * 🕒 Meaning: Issued at (seconds since epoch).
     * <br>📜 RFC / Spec: <a href="https://www.rfc-editor.org/rfc/rfc7519#section-4.1.6">RFC 7519 §4.1.6</a>
     * <br>🧩 Type: NumericDate (long)
     * <br>🧪 Example:
     * <pre>{@code
     * 1729993200
     * }</pre>
     */
    public static final String CLAIM_ISSUED_AT = "iat";

    /**
     * 🆔 Meaning: JWT ID — unique identifier for the token.
     * <br>📜 RFC / Spec: <a href="https://www.rfc-editor.org/rfc/rfc7519#section-4.1.7">RFC 7519 §4.1.7</a>
     * <br>🧩 Type: String
     * <br>🧪 Example:
     * <pre>{@code
     * "7f3d34d4-6b23-44d3-a0f4-1b823dfddfa2"
     * }</pre>
     */
    public static final String CLAIM_JWT_ID = "jti";

    // =============================================================================================
    // 🌍 OIDC / OAuth 2.0 COMMON EXTENSIONS
    // =============================================================================================

    /**
     * ⏱️ Meaning: Time when the end-user authentication occurred.
     * <br>📜 RFC / Spec: <a href="https://openid.net/specs/openid-connect-core-1_0.html#IDToken">OpenID Connect Core §2 (ID Token)</a>
     * <br>🧩 Type: NumericDate (long)
     * <br>🧪 Example:
     * <pre>{@code
     * 1729993200
     * }</pre>
     */
    public static final String CLAIM_AUTHENTICATION_TIME = "auth_time";

    /**
     * 🔁 Meaning: Nonce to associate a client session with the ID Token to mitigate replay.
     * <br>📜 RFC / Spec: <a href="https://openid.net/specs/openid-connect-core-1_0.html#NonceNotes">OpenID Connect Core (Nonce)</a>
     * <br>🧩 Type: String
     * <br>🧪 Example:
     * <pre>{@code
     * "n-0S6_WzA2Mj"
     * }</pre>
     */
    public static final String CLAIM_NONCE = "nonce";

    /**
     * 🧭 Meaning: Authentication Context Class Reference.
     * <br>📜 RFC / Spec: <a href="https://openid.net/specs/openid-connect-core-1_0.html#IDToken">OpenID Connect Core §2 (ID Token)</a>
     * <br>🧩 Type: String
     * <br>🧪 Example:
     * <pre>{@code
     * "urn:mace:incommon:iap:silver"
     * }</pre>
     */
    public static final String CLAIM_AUTHENTICATION_CONTEXT_CLASS_REFERENCE = "acr";

    /**
     * 🧰 Meaning: Authentication Methods References.
     * <br>📜 RFC / Spec: <a href="https://openid.net/specs/openid-connect-core-1_0.html#IDToken">OpenID Connect Core §2 (ID Token)</a>
     * <br>🧩 Type: Array of String
     * <br>🧪 Example:
     * <pre>{@code
     * ["pwd","otp"]
     * }</pre>
     */
    public static final String CLAIM_AUTHENTICATION_METHODS_REFERENCES = "amr";

    /**
     * 🧾 Meaning: Authorized party — the party to which the ID Token was issued (client_id).
     * <br>📜 RFC / Spec: <a href="https://openid.net/specs/openid-connect-core-1_0.html#IDToken">OpenID Connect Core §2 (ID Token)</a>
     * <br>🧩 Type: String
     * <br>🧪 Example:
     * <pre>{@code
     * "dashboard-service"
     * }</pre>
     */
    public static final String CLAIM_AUTHORIZED_PARTY = "azp";

    /**
     * 🪪 Meaning: Access token hash (left-most half of hash).
     * <br>📜 RFC / Spec: <a href="https://openid.net/specs/openid-connect-core-1_0.html#CodeIDToken">OpenID Connect Core §3.3.2.11</a>
     * <br>🧩 Type: String
     * <br>🧪 Example:
     * <pre>{@code
     * "LDktKdoQak3Pk0cnXxCltA"
     * }</pre>
     */
    public static final String CLAIM_ACCESS_TOKEN_HASH = "at_hash";

    /**
     * 🔐 Meaning: Authorization code hash (left-most half of hash).
     * <br>📜 RFC / Spec: <a href="https://openid.net/specs/openid-connect-core-1_0.html#CodeIDToken">OpenID Connect Core §3.3.2.11</a>
     * <br>🧩 Type: String
     * <br>🧪 Example:
     * <pre>{@code
     * "Qcb0...abc"
     * }</pre>
     */
    public static final String CLAIM_AUTHORIZATION_CODE_HASH = "c_hash";

    /**
     * 🔏 Meaning: State hash (used in some profiles e.g., DPoP/Front-Channel Logout).
     * <br>📜 RFC / Spec: OpenID Connect Front-Channel Logout / related profiles
     * <br>🧩 Type: String
     * <br>🧪 Example:
     * <pre>{@code
     * "SHashValue"
     * }</pre>
     */
    public static final String CLAIM_STATE_HASH = "s_hash";

    /**
     * 🧩 Meaning: OAuth 2.0 client identifier.
     * <br>📜 RFC / Spec: OAuth 2.0 / OIDC practice
     * <br>🧩 Type: String
     * <br>🧪 Example:
     * <pre>{@code
     * "client-123"
     * }</pre>
     */
    public static final String CLAIM_CLIENT_ID = "client_id";

    /**
     * 🎫 Meaning: Space-delimited OAuth 2.0 scopes (or vendor {@code scp}).
     * <br>📜 RFC / Spec: OAuth 2.0 practice
     * <br>🧩 Type: String
     * <br>🧪 Example:
     * <pre>{@code
     * "openid profile email"
     * }</pre>
     */
    public static final String CLAIM_SCOPE = "scope";

    /**
     * 🛡️ Meaning: Roles associated with the subject (de-facto).
     * <br>📜 RFC / Spec: Vendor / app specific
     * <br>🧩 Type: Array of String
     * <br>🧪 Example:
     * <pre>{@code
     * ["ADMIN","USER"]
     * }</pre>
     */
    public static final String CLAIM_ROLES = "roles";

    /**
     * 🔓 Meaning: Permissions/authorities associated with the subject (de-facto).
     * <br>📜 RFC / Spec: Vendor / app specific
     * <br>🧩 Type: Array of String
     * <br>🧪 Example:
     * <pre>{@code
     * ["order:read","order:write"]
     * }</pre>
     */
    public static final String CLAIM_PERMISSIONS = "permissions";

    /**
     * 👥 Meaning: Groups to which the subject belongs (de-facto).
     * <br>📜 RFC / Spec: Vendor / app specific
     * <br>🧩 Type: Array of String
     * <br>🧪 Example:
     * <pre>{@code
     * ["engineering","admins"]
     * }</pre>
     */
    public static final String CLAIM_GROUPS = "groups";

    /**
     * 🏢 Meaning: Multi-tenant / organization identifier (de-facto).
     * <br>📜 RFC / Spec: Vendor / app specific
     * <br>🧩 Type: String
     * <br>🧪 Example:
     * <pre>{@code
     * "tenant-alpha"
     * }</pre>
     */
    public static final String CLAIM_TENANT = "tenant";

    /**
     * 🔗 Meaning: Confirmation claim (binds a token to a key — mTLS/DPoP).
     * <br>📜 RFC / Spec: <a href="https://www.rfc-editor.org/rfc/rfc7800">RFC 7800</a>
     * <br>🧩 Type: JSON object
     * <br>🧪 Example:
     * <pre>{@code
     * { "x5t#S256":"...", "jkt":"..." }
     * }</pre>
     */
    public static final String CLAIM_CONFIRMATION = "cnf";

    /**
     * 🧑‍🤝‍🧑 Meaning: Actor claim for delegated authorization.
     * <br>📜 RFC / Spec: <a href="https://www.rfc-editor.org/rfc/rfc8693#section-4.2">RFC 8693 §4.2</a>
     * <br>🧩 Type: JSON object
     * <br>🧪 Example:
     * <pre>{@code
     * { "sub":"service-account" }
     * }</pre>
     */
    public static final String CLAIM_ACTOR = "act";

    public static class Builder {

        private final Map<String, Object> headers = new LinkedHashMap<>();
        private final Map<String, Object> claims  = new LinkedHashMap<>();
        private       String              token;
        private       Instant             issuedAt;
        private       Instant             expiresAt;
        private       String              subject;

        public Builder token(String token) {
            this.token = token;
            return this;
        }

        public Builder header(String name, Object value) {
            this.headers.put(name, value);
            return this;
        }

        public Builder headers(Map<String, Object> headers) {
            this.headers.putAll(headers);
            return this;
        }

        public Builder algorithm(String alg) {
            return header(HEADER_ALGORITHM, alg);
        }

        public Builder type(String typ) {
            return header(HEADER_TYPE, typ);
        }

        public Builder contentType(String cty) {
            return header(HEADER_CONTENT_TYPE, cty);
        }

        public Builder keyId(String kid) {
            return header(HEADER_KEY_ID, kid);
        }

        public Builder jwkSetUrl(String jku) {
            return header(HEADER_JWK_SET_URL, jku);
        }

        public Builder jsonWebKey(Map<String, Object> jwk) {
            return header(HEADER_JSON_WEB_KEY, jwk);
        }

        public Builder x509Url(String x5u) {
            return header(HEADER_X509_URL, x5u);
        }

        public Builder x509CertificateChain(List<String> x5c) {
            return header(HEADER_X509_CERTIFICATE_CHAIN, x5c);
        }

        public Builder x509Sha1Thumbprint(String x5t) {
            return header(HEADER_X509_CERTIFICATE_SHA1_THUMBPRINT, x5t);
        }

        public Builder x509Sha256Thumbprint(String x5tS256) {
            return header(HEADER_X509_CERTIFICATE_SHA256_THUMBPRINT, x5tS256);
        }

        public Builder critical(List<String> crit) {
            return header(HEADER_CRITICAL, crit);
        }

        public Builder encryptionAlgorithm(String enc) {
            return header(HEADER_ENCRYPTION_ALGORITHM, enc);
        }

        public Builder compressionAlgorithm(String zip) {
            return header(HEADER_COMPRESSION_ALGORITHM, zip);
        }

        public Builder claim(String name, Object value) {
            this.claims.put(name, value);
            return this;
        }
        public Builder claims(Map<String, Object> claims) {
            this.claims.putAll(claims);
            return this;
        }

        public Builder issuer(String iss) {
            return claim(CLAIM_ISSUER, iss);
        }

        public Builder subject(String sub) {
            this.subject = sub;
            return claim(CLAIM_SUBJECT, sub);
        }

        public Builder audience(String... aud) {
            return claim(CLAIM_AUDIENCE, aud == null ? null : List.of(aud));
        }

        public Builder jwtId(String jti) {
            return claim(CLAIM_JWT_ID, jti);
        }

        public Builder issuedAt(Instant iat) {
            this.issuedAt = iat;
            return claim(CLAIM_ISSUED_AT, iat.getEpochSecond());
        }

        public Builder notBefore(Instant nbf) {
            return claim(CLAIM_NOT_BEFORE, nbf.getEpochSecond());
        }

        public Builder expiresAt(Instant exp) {
            this.expiresAt = exp;
            return claim(CLAIM_EXPIRATION_TIME, exp.getEpochSecond());
        }

        // OIDC/OAuth helpers
        public Builder authenticationTime(Instant authTime) {
            return claim(CLAIM_AUTHENTICATION_TIME, authTime.getEpochSecond());
        }

        public Builder nonce(String nonce) {
            return claim(CLAIM_NONCE, nonce);
        }

        public Builder authenticationContextClassReference(String acr) {
            return claim(CLAIM_AUTHENTICATION_CONTEXT_CLASS_REFERENCE, acr);
        }

        public Builder authenticationMethodsReferences(List<String> amr) {
            return claim(CLAIM_AUTHENTICATION_METHODS_REFERENCES, amr);
        }

        public Builder authorizedParty(String azp) {
            return claim(CLAIM_AUTHORIZED_PARTY, azp);
        }

        public Builder accessTokenHash(String atHash) {
            return claim(CLAIM_ACCESS_TOKEN_HASH, atHash);
        }

        public Builder authorizationCodeHash(String cHash) {
            return claim(CLAIM_AUTHORIZATION_CODE_HASH, cHash);
        }

        public Builder stateHash(String sHash) {
            return claim(CLAIM_STATE_HASH, sHash);
        }

        public Builder clientId(String clientId) {
            return claim(CLAIM_CLIENT_ID, clientId);
        }

        public Builder scope(String scope) {
            return claim(CLAIM_SCOPE, scope);
        }

        public Builder roles(List<String> roles) {
            return claim(CLAIM_ROLES, roles);
        }

        public Builder permissions(List<String> permissions) {
            return claim(CLAIM_PERMISSIONS, permissions);
        }

        public Builder groups(List<String> groups) {
            return claim(CLAIM_GROUPS, groups);
        }

        public Builder tenant(String tenant) {
            return claim(CLAIM_TENANT, tenant);
        }

        public Builder confirmation(Map<String, Object> cnf) {
            return claim(CLAIM_CONFIRMATION, cnf);
        }

        public Builder actor(Map<String, Object> act) {
            return claim(CLAIM_ACTOR, act);
        }

        // Build
        public Jwt build() {
            if (issuedAt == null && claims.containsKey(CLAIM_ISSUED_AT)) {
                issuedAt = Instant.ofEpochSecond(((Number) claims.get(CLAIM_ISSUED_AT)).longValue());
            }
            if (expiresAt == null && claims.containsKey(CLAIM_EXPIRATION_TIME)) {
                expiresAt = Instant.ofEpochSecond(((Number) claims.get(CLAIM_EXPIRATION_TIME)).longValue());
            }
            if (subject == null && claims.containsKey(CLAIM_SUBJECT)) {
                subject = String.valueOf(claims.get(CLAIM_SUBJECT));
            }

            return new Jwt(
                    token,
                    Collections.unmodifiableMap(new LinkedHashMap<>(headers)),
                    Collections.unmodifiableMap(new LinkedHashMap<>(claims)),
                    issuedAt, expiresAt, subject
            );
        }
    }
}
