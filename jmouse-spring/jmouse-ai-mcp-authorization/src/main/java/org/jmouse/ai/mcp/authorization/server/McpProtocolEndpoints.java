package org.jmouse.ai.mcp.authorization.server;

import org.jmouse.ai.mcp.authorization.AuthorizationRoutes;
import org.jmouse.ai.mcp.authorization.AuthorizationVocabulary;
import org.jmouse.ai.mcp.authorization.McpAuthorizationException;
import org.jmouse.ai.mcp.authorization.ProofKeyPolicy;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.List;

/**
 * Every step a client takes before it holds a credential: announcing itself, being sent to the consent
 * screen, and redeeming its one-time code.
 *
 * <p>All three are open by necessity and none of them grants anything on its own. Registration hands out
 * a label; authorization hands the browser to a screen where a <strong>person</strong> decides; the token
 * endpoint spends a code that only exists because somebody already approved it.
 *
 * <p>⚠️ <strong>Where a refusal goes is a rule, not a preference.</strong> A bad redirect address is
 * answered <em>here</em>, in the response body, because sending an error to an address we have just
 * refused to trust is the open redirect the allow-list exists to prevent. Every other refusal goes back
 * to the client's own vetted address, where its code can read it and correct itself.
 *
 * <p>⚠️ <strong>The paths are placeholders resolved at startup</strong>, because a product decides where
 * its API lives and a registered client has already discovered whatever answer it gave. See
 * {@link McpAuthorizationProperties#PROTOCOL_PREFIX_EXPRESSION}.
 */
@RestController
public class McpProtocolEndpoints {

    private final ClientAuthorizationFlow    flow;
    private final ClientNameRegistry         clientRegistry;
    private final McpAuthorizationProperties properties;

    public McpProtocolEndpoints(
            ClientAuthorizationFlow    flow,
            ClientNameRegistry         clientRegistry,
            McpAuthorizationProperties properties
    ) {
        this.flow           = flow;
        this.clientRegistry = clientRegistry;
        this.properties     = properties;
    }

    // ── Registration ─────────────────────────────────────────────────────────────

    @PostMapping(McpAuthorizationProperties.PROTOCOL_PREFIX_EXPRESSION + "/register")
    public ResponseEntity<AuthorizationDtos.ClientRegistrationResponse> register(
            @RequestBody AuthorizationDtos.ClientRegistrationRequest request
    ) {
        String clientId = registered(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new AuthorizationDtos.ClientRegistrationResponse(
                        clientId,
                        Instant.now().getEpochSecond(),
                        clientRegistry.nameOf(clientId),
                        request.redirectUris(),
                        List.of(AuthorizationVocabulary.AUTHORIZATION_CODE,
                                AuthorizationVocabulary.REFRESH_TOKEN),
                        List.of(AuthorizationVocabulary.RESPONSE_TYPE_CODE),
                        AuthorizationVocabulary.NO_CLIENT_AUTHENTICATION,
                        AuthorizationRoutes.SCOPE));
    }

    // ── Authorization ────────────────────────────────────────────────────────────

    /**
     * Vets what the client is asking for and sends the browser on to the consent screen.
     *
     * <p>⚠️ The {@code resource} parameter (RFC 8707) is accepted and ignored: an installation protecting
     * exactly one resource has nothing for a client to narrow to, and refusing over how it spelled the
     * address would strand a client that had everything else right.
     */
    @GetMapping(McpAuthorizationProperties.PROTOCOL_PREFIX_EXPRESSION + "/authorize")
    public ResponseEntity<Void> authorize(
            @RequestParam(name = "redirect_uri",          required = false) String redirectUri,
            @RequestParam(name = "response_type",         required = false) String responseType,
            @RequestParam(name = "client_id",             required = false) String clientId,
            @RequestParam(name = "state",                 required = false) String state,
            @RequestParam(name = "code_challenge",        required = false) String codeChallenge,
            @RequestParam(name = "code_challenge_method", required = false) String codeChallengeMethod
    ) {
        URI target = acceptableRedirectUri(redirectUri);

        if (!AuthorizationVocabulary.RESPONSE_TYPE_CODE.equals(responseType)) {
            return refuseToClient(target, state, AuthorizationVocabulary.ERROR_UNSUPPORTED_RESPONSE_TYPE,
                    "Only the authorization code flow is supported, so response_type must be 'code'.");
        }

        if (codeChallenge == null || codeChallenge.isBlank()) {
            return refuseToClient(target, state, AuthorizationVocabulary.ERROR_INVALID_REQUEST,
                    "A code_challenge is required: a credential is never issued to a client that cannot "
                  + "prove later that it started the request.");
        }

        if (!ProofKeyPolicy.SUPPORTED_METHOD.equals(codeChallengeMethod)) {
            return refuseToClient(target, state, AuthorizationVocabulary.ERROR_INVALID_REQUEST,
                    "code_challenge_method must be S256; a plain challenge travels in the same request as "
                  + "the thing it is meant to protect.");
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(consentScreenFor(clientId, redirectUri, state, codeChallenge))
                .build();
    }

    // ── Token ────────────────────────────────────────────────────────────────────

    /**
     * Spends a code, or renews a credential. Form-encoded, because that is what OAuth's token endpoint is
     * and what every client sends.
     */
    @PostMapping(value = McpAuthorizationProperties.PROTOCOL_PREFIX_EXPRESSION + "/token",
                 consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public AuthorizationDtos.TokenResponse token(
            @RequestParam(name = "grant_type",    required = false) String grantType,
            @RequestParam(name = "code",          required = false) String code,
            @RequestParam(name = "code_verifier", required = false) String codeVerifier,
            @RequestParam(name = "refresh_token", required = false) String refreshToken
    ) {
        if (AuthorizationVocabulary.AUTHORIZATION_CODE.equals(grantType)) {
            return asTokenResponse(flow.exchangeCode(code, codeVerifier));
        }

        if (AuthorizationVocabulary.REFRESH_TOKEN.equals(grantType)) {
            return asTokenResponse(flow.renew(refreshToken));
        }

        throw new UnsupportedGrantException("grant_type must be '"
                + AuthorizationVocabulary.AUTHORIZATION_CODE + "' or '"
                + AuthorizationVocabulary.REFRESH_TOKEN + "'. Received: " + grantType);
    }

    // ── Refusals in the shape a client can act on ────────────────────────────────

    @ExceptionHandler(McpAuthorizationException.class)
    public ResponseEntity<AuthorizationDtos.ErrorResponse> onRefusedRequest(McpAuthorizationException refusal) {
        return errorBody(AuthorizationVocabulary.ERROR_INVALID_GRANT, refusal.getMessage());
    }

    @ExceptionHandler(UnsupportedGrantException.class)
    public ResponseEntity<AuthorizationDtos.ErrorResponse> onUnsupportedGrant(UnsupportedGrantException refusal) {
        return errorBody(AuthorizationVocabulary.ERROR_UNSUPPORTED_GRANT_TYPE, refusal.getMessage());
    }

    @ExceptionHandler(InvalidRedirectUriException.class)
    public ResponseEntity<AuthorizationDtos.ErrorResponse> onInvalidRedirectUri(InvalidRedirectUriException refusal) {
        return errorBody(AuthorizationVocabulary.ERROR_INVALID_REDIRECT_URI, refusal.getMessage());
    }

    // ── Internal ─────────────────────────────────────────────────────────────────

    /** The consent screen is served on the address a <em>person</em> reaches this product at. */
    private URI consentScreenFor(String clientId, String redirectUri, String state, String codeChallenge) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(properties.browserUrl(properties.routes().consentRoute()))
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", ProofKeyPolicy.SUPPORTED_METHOD);

        if (state != null && !state.isBlank()) {
            builder.queryParam("state", state);
        }

        return builder.build().encode().toUri();
    }

    /** A refusal the client reads at its own address, having proved that address is loopback. */
    private ResponseEntity<Void> refuseToClient(URI target, String state, String error, String description) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUri(target)
                .queryParam("error", error)
                .queryParam("error_description", description);

        if (state != null && !state.isBlank()) {
            builder.queryParam("state", state);
        }

        return ResponseEntity.status(HttpStatus.FOUND).location(builder.build().encode().toUri()).build();
    }

    /**
     * A refused address answers {@code invalid_redirect_uri} rather than the generic
     * {@code invalid_grant} a client cannot do anything specific with.
     */
    private String registered(AuthorizationDtos.ClientRegistrationRequest request) {
        try {
            return flow.register(request.clientName(), request.redirectUris());

        } catch (McpAuthorizationException refusal) {
            throw new InvalidRedirectUriException(refusal.getMessage());
        }
    }

    private URI acceptableRedirectUri(String candidate) {
        try {
            return flow.requireAcceptableRedirectUri(candidate);

        } catch (McpAuthorizationException refusal) {
            throw new InvalidRedirectUriException(refusal.getMessage());
        }
    }

    private AuthorizationDtos.TokenResponse asTokenResponse(CredentialIssuer.IssuedCredential credential) {
        return new AuthorizationDtos.TokenResponse(
                credential.accessToken(),
                AuthorizationVocabulary.BEARER_TOKEN_TYPE,
                credential.expiresInSeconds(),
                credential.refreshToken(),
                AuthorizationRoutes.SCOPE);
    }

    private ResponseEntity<AuthorizationDtos.ErrorResponse> errorBody(String error, String description) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new AuthorizationDtos.ErrorResponse(error, description));
    }

    /** A grant type this server does not implement — distinct from one it refused. */
    private static class UnsupportedGrantException extends RuntimeException {

        UnsupportedGrantException(String message) {
            super(message);
        }
    }

    /** An address this server will not deliver a live authorization code to. */
    private static class InvalidRedirectUriException extends RuntimeException {

        InvalidRedirectUriException(String message) {
            super(message);
        }
    }
}
