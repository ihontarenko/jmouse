package org.jmouse.ai.mcp.authorization.server;

import org.jmouse.ai.mcp.authorization.AuthorizationCodeStore;
import org.jmouse.ai.mcp.authorization.LoopbackRedirectPolicy;
import org.jmouse.ai.mcp.authorization.McpAuthorizationException;
import org.jmouse.ai.mcp.authorization.ProofKeyPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Instant;
import java.util.List;

/**
 * Walking the protocol, and nothing else.
 *
 * <p>Two things stay apart here and must not be collapsed. <em>Authentication</em> — whoever is signed
 * in, by whatever means — happened long before this class is reached and is no concern of it.
 * <em>Authorization</em> — may this client act as this thing — is the only decision made here, and it
 * belongs to the person the screen is in front of.
 *
 * <p>⚠️ <strong>Nothing that arrives from the consent screen is trusted for having been through
 * it.</strong> {@link #review} and {@link #approve} are independent requests and only the second mints
 * anything, so the address and the proof are vetted again in both. A screen is where a person is asked;
 * it is never where a request is validated.
 *
 * <p>⚠️ <strong>The credential is minted on redemption, not on approval.</strong> An approval nobody came
 * back for leaves no live token and no row to clean up, and the code that <em>is</em> redeemed proves
 * possession of the verifier the client committed to before anybody was asked anything.
 */
public class ClientAuthorizationFlow {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientAuthorizationFlow.class);

    private final McpAuthorizationProperties properties;
    private final LoopbackRedirectPolicy     redirectPolicy;
    private final ProofKeyPolicy             proofKeyPolicy;
    private final AuthorizationCodeStore     codeStore;
    private final ClientNameRegistry         clientRegistry;
    private final ApprovingSubject           approvingSubject;
    private final CredentialIssuer           credentialIssuer;

    public ClientAuthorizationFlow(
            McpAuthorizationProperties properties,
            LoopbackRedirectPolicy     redirectPolicy,
            ProofKeyPolicy             proofKeyPolicy,
            AuthorizationCodeStore     codeStore,
            ClientNameRegistry         clientRegistry,
            ApprovingSubject           approvingSubject,
            CredentialIssuer           credentialIssuer
    ) {
        this.properties       = properties;
        this.redirectPolicy   = redirectPolicy;
        this.proofKeyPolicy   = proofKeyPolicy;
        this.codeStore        = codeStore;
        this.clientRegistry   = clientRegistry;
        this.approvingSubject = approvingSubject;
        this.credentialIssuer = credentialIssuer;
    }

    /**
     * Announces a client and answers with the identifier it will carry.
     *
     * <p>The addresses are vetted by the same allow-list the flow itself uses, so a client learns its
     * address is unacceptable while it is still setting up — rather than after a person has approved a
     * screen for a code that could never be delivered.
     */
    public String register(String clientName, List<String> redirectUris) {
        if (redirectUris == null || redirectUris.isEmpty()) {
            throw new McpAuthorizationException("A client must register at least one redirect address.");
        }

        redirectUris.forEach(redirectPolicy::require);

        String clientId = clientRegistry.register(clientName);

        LOGGER.info("Registered client '{}' as {}", clientRegistry.nameOf(clientId), clientId);

        return clientId;
    }

    /**
     * What the approval screen shows.
     *
     * <p>Everything that could be refused is refused here, before the person is asked anything — an
     * approval that cannot be honoured is worse than a refusal, because they have already said yes to it.
     */
    public AuthorizationDtos.ClientReviewResponse review(AuthorizationDtos.ClientReviewRequest request) {
        URI target = requireHonourableRequest(
                request.redirectUri(), request.codeChallenge(), request.codeChallengeMethod());

        ApprovingSubject.Approver approver = approvingSubject.current();

        return new AuthorizationDtos.ClientReviewResponse(
                properties.getApplicationName(),
                clientRegistry.nameOf(request.clientId()),
                redirectPolicy.describe(target),
                approver.displayName(),
                approver.detail(),
                approver.choices().stream().map(AuthorizationDtos.SubjectView::from).toList(),
                properties.getCodeLifetime().toSeconds());
    }

    /** Records the approval and answers with where the browser should go next. */
    public AuthorizationDtos.ClientApprovalResponse approve(AuthorizationDtos.ClientApprovalRequest request) {
        URI target = requireHonourableRequest(
                request.redirectUri(), request.codeChallenge(), request.codeChallengeMethod());

        ApprovingSubject.Choice chosen = requireOwnChoice(request.subjectReference());
        String                  clientName = clientRegistry.nameOf(request.clientId());

        String code = codeStore.issue(new AuthorizationCodeStore.PendingAuthorization(
                request.clientId(),
                target,
                request.codeChallenge(),
                chosen.reference(),
                Instant.now().plus(properties.getCodeLifetime())));

        LOGGER.info("Client '{}' authorized to act as {}, code returning to {}",
                clientName, chosen.name(), redirectPolicy.describe(target));

        return new AuthorizationDtos.ClientApprovalResponse(
                clientName, redirectPolicy.callbackUrl(target, code, request.state()));
    }

    /** Spends a one-time code for the credential it was approved for. */
    public CredentialIssuer.IssuedCredential exchangeCode(String code, String codeVerifier) {
        AuthorizationCodeStore.PendingAuthorization authorization = codeStore.redeem(code)
                .orElseThrow(() -> new McpAuthorizationException(
                        "This authorization code is unknown, expired or already used. A code lasts "
                      + properties.getCodeLifetime().toSeconds() + " seconds and works once — start the "
                      + "authorization again."));

        proofKeyPolicy.requireMatchingVerifier(authorization.challenge(), codeVerifier);

        return credentialIssuer.issue(new CredentialIssuer.ApprovedAuthorization(
                authorization.subjectReference(),
                clientRegistry.nameOf(authorization.clientId())));
    }

    /** Renews a credential a client already holds. */
    public CredentialIssuer.IssuedCredential renew(String refreshToken) {
        return credentialIssuer.renew(refreshToken);
    }

    /** Vets a redirect address on its own, for the one caller that has to refuse it differently. */
    public URI requireAcceptableRedirectUri(String candidate) {
        return redirectPolicy.require(candidate);
    }

    /**
     * The vetting both halves of the flow share: an address we are willing to send a live code to, and a
     * client that has committed to a verifier it can prove later.
     */
    private URI requireHonourableRequest(String redirectUri, String codeChallenge, String codeChallengeMethod) {
        proofKeyPolicy.requireChallenge(codeChallenge, codeChallengeMethod);

        return redirectPolicy.require(redirectUri);
    }

    /**
     * ⚠️ <strong>The one check standing between a person and authorizing something that is not
     * theirs.</strong> The reference arrives in a request body, so it is matched against what
     * {@link ApprovingSubject} says this caller may act as rather than taken at its word — and a product
     * that answers that question too widely has widened it here too, which is why the interface says so.
     *
     * <p>A single choice is taken as the answer when none is named, so a product with nothing to pick
     * needs no picker and its screen has no radio buttons on it.
     */
    private ApprovingSubject.Choice requireOwnChoice(String reference) {
        List<ApprovingSubject.Choice> choices = approvingSubject.current().choices();

        if (choices.isEmpty()) {
            throw new McpAuthorizationException(
                    "There is nothing here for a client to act as, so nothing can be approved.");
        }

        if (reference == null || reference.isBlank()) {
            if (choices.size() == 1) {
                return requireAvailable(choices.getFirst());
            }

            throw new McpAuthorizationException(
                    "This request did not say what the client should act as, and there is more than one "
                  + "thing it could be. Choose one on the approval screen.");
        }

        return requireAvailable(choices.stream()
                .filter(choice -> reference.equals(choice.reference()))
                .findFirst()
                .orElseThrow(() -> new McpAuthorizationException(
                        "That is not something this account may authorize a client to act as.")));
    }

    private ApprovingSubject.Choice requireAvailable(ApprovingSubject.Choice choice) {
        if (!choice.available()) {
            throw new McpAuthorizationException(choice.unavailableReason() == null
                    ? "'" + choice.name() + "' cannot be used right now, so no credential can be issued "
                      + "for it."
                    : choice.unavailableReason());
        }

        return choice;
    }
}
