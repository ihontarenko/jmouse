package org.jmouse.ai.mcp.authorization.server;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Every body this module reads or writes.
 *
 * <p>⚠️ <strong>Each field name is spelled out rather than left to a naming strategy.</strong> The
 * RFC-shaped ones are not anybody's to design — RFC 7591 and RFC 6749 read by clients that will not look
 * for a camel-cased spelling of a name written with underscores — and the consent-screen ones are read
 * by a page this module ships, which must keep working when the product hosting it changes how it
 * serialises everything else.
 *
 * <p>Nulls are dropped throughout: an absent optional field means "not supported", while a null one is a
 * parse hazard on the reading side for no gain.
 */
public final class AuthorizationDtos {

    private AuthorizationDtos() {
    }

    // ── What a client sends and reads (RFC 7591, RFC 6749) ───────────────────────

    /** A client announcing itself. Everything in it is a claim; only the addresses are vetted. */
    public record ClientRegistrationRequest(

            @JsonProperty("client_name")
            String clientName,

            @JsonProperty("redirect_uris")
            List<String> redirectUris
    ) {}

    /** What a client gets back for announcing itself. Public client, so no secret is issued. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ClientRegistrationResponse(

            @JsonProperty("client_id")
            String clientId,

            @JsonProperty("client_id_issued_at")
            long clientIdIssuedAt,

            @JsonProperty("client_name")
            String clientName,

            @JsonProperty("redirect_uris")
            List<String> redirectUris,

            @JsonProperty("grant_types")
            List<String> grantTypes,

            @JsonProperty("response_types")
            List<String> responseTypes,

            @JsonProperty("token_endpoint_auth_method")
            String tokenEndpointAuthMethod,

            @JsonProperty("scope")
            String scope
    ) {}

    /** The credential itself, in the spelling the specification uses. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TokenResponse(

            @JsonProperty("access_token")
            String accessToken,

            @JsonProperty("token_type")
            String tokenType,

            @JsonProperty("expires_in")
            long expiresIn,

            @JsonProperty("refresh_token")
            String refreshToken,

            @JsonProperty("scope")
            String scope
    ) {}

    /**
     * A refusal in the shape a client can act on.
     *
     * <p>This is not decoration. A client reading an unexpected error body cannot tell "your code
     * expired, start again" from "this server is broken", and the difference decides whether it
     * re-authorizes or gives up — which is precisely how a hand-rolled error body strands somebody who
     * did nothing wrong.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorResponse(

            @JsonProperty("error")
            String error,

            @JsonProperty("error_description")
            String errorDescription
    ) {}

    // ── What the consent screen sends and reads ──────────────────────────────────

    /** The client's parameters, exactly as they arrived in the consent URL. */
    public record ClientReviewRequest(

            @JsonProperty("client_id")
            String clientId,

            @JsonProperty("redirect_uri")
            String redirectUri,

            @JsonProperty("code_challenge")
            String codeChallenge,

            @JsonProperty("code_challenge_method")
            String codeChallengeMethod,

            @JsonProperty("state")
            String state
    ) {}

    /** The same, once a person has named what the client may act as. */
    public record ClientApprovalRequest(

            @JsonProperty("client_id")
            String clientId,

            @JsonProperty("redirect_uri")
            String redirectUri,

            @JsonProperty("code_challenge")
            String codeChallenge,

            @JsonProperty("code_challenge_method")
            String codeChallengeMethod,

            @JsonProperty("state")
            String state,

            /** ⚠️ Opaque, and re-checked against what this person may actually authorize. */
            @JsonProperty("subject_reference")
            String subjectReference
    ) {}

    /**
     * What the screen shows.
     *
     * <p>Reaching this response at all means the redirect address and the client's proof have already
     * passed — the screen never offers an approval that would then be refused.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ClientReviewResponse(

            @JsonProperty("application_name")
            String applicationName,

            @JsonProperty("client_name")
            String clientName,

            /** Where a live authorization code will be sent, already vetted as loopback. */
            @JsonProperty("redirect_target")
            String redirectTarget,

            @JsonProperty("approver_name")
            String approverName,

            @JsonProperty("approver_detail")
            String approverDetail,

            @JsonProperty("subjects")
            List<SubjectView> subjects,

            @JsonProperty("code_lifetime_seconds")
            long codeLifetimeSeconds
    ) {}

    /** One thing the client could be authorized to act as. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record SubjectView(

            @JsonProperty("reference")
            String reference,

            @JsonProperty("name")
            String name,

            @JsonProperty("detail")
            String detail,

            @JsonProperty("available")
            boolean available,

            @JsonProperty("unavailable_reason")
            String unavailableReason
    ) {

        static SubjectView from(ApprovingSubject.Choice choice) {
            return new SubjectView(choice.reference(), choice.name(), choice.detail(),
                                   choice.available(), choice.unavailableReason());
        }
    }

    /** The approval, and where the browser goes next. */
    public record ClientApprovalResponse(

            @JsonProperty("client_name")
            String clientName,

            /** ⚠️ A full URL on the client's own machine. The page assigns it; nothing fetches it. */
            @JsonProperty("redirect_url")
            String redirectUrl
    ) {}
}
