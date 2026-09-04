package org.jmouse.ai.mcp.authorization.server;

import org.jmouse.ai.agent.Agent;
import org.jmouse.ai.agent.AgentAdmission;
import org.jmouse.ai.agent.AgentConnection;
import org.jmouse.ai.agent.AgentConnections;
import org.jmouse.ai.agent.AgentDirectory;
import org.jmouse.ai.mcp.authorization.AuthorizationRoutes;
import org.jmouse.ai.mcp.authorization.McpAuthorizationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * The one place a credential that reaches a protocol endpoint is minted, renewed, or ended.
 *
 * <h2>⚠️ IT IS NOT NAMED AFTER WHOM IT ISSUES TO, and that is enforced</h2>
 *
 * <p>This was {@code AgentCredentials}, with a nested {@code ActingAgent}, and the module's own
 * architecture test refuses the word: <em>one product issues to a sub-account holding its own
 * permissions and another to the approving person, so a module that knew the difference would be
 * wrong for one of them.</em> Whom a credential is issued against is the product's fact and reaches
 * here only as an opaque reference — see {@code ApprovingSubject}.
 *
 * <p>The forbidden words are {@code account}, {@code user}, {@code member} and {@code agent}, checked
 * against every type name in this package. ⚠️ Only type <em>names</em> — the {@code Agent} rows this
 * class reads still come from {@code org.jmouse.ai.agent}, because holding a product's row is not the
 * same as being named after one.
 *
 * <h2>⚠️ This class existed three times before it existed once</h2>
 *
 * <p>Identity, Tessera and WiQ each wrote it — 258, 366 and 381 lines — and the three agreed on every
 * decision that mattered: the claims, the rotation, the sliding window, the four refusals, the two
 * separate revocations. What they did <em>not</em> agree on was trivia, and the divergence was already
 * starting: one had grown an administrator's listing the others lacked. A rule about credentials that
 * lives in three files is a rule that is about to be three different rules.
 *
 * <p>⚠️ <strong>What differs between products is {@link ProtocolTokenMinter} and nothing else.</strong>
 * Two of them sign an HS256 token with a secret only that product holds; the third mints an ordinary
 * product JWT and confines it with a gate on the route. Everything around that — enrolment, the four
 * refusals, rotation, the sliding window, revocation — is the same in all of them, and is here.
 *
 * <p>⚠️ <strong>The owner is {@link Agent#ownerReference()} and never a parent-child mirror.</strong> Every
 * product carrying one has an ADR saying so; here the types make it hard rather than the discipline,
 * because a mirror is not reachable from this class at all.
 */
public class ProtocolCredentials {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProtocolCredentials.class);

    /**
     * Names the connection an access token was issued against, so revocation can reach it.
     *
     * <p>⚠️ Kept here as well as on {@link SignedTokenMinter} because a product's own validator reads it
     * off an arriving token, and that validator has no business knowing which minter produced it.
     */
    public static final String CREDENTIAL_CLAIM = SignedTokenMinter.CREDENTIAL_CLAIM;

    /** And the agent it acts as, so attribution does not cost a second lookup. */
    public static final String AGENT_CLAIM = SignedTokenMinter.AGENT_CLAIM;

    /** What the credential is for, in the one word the discovery documents also publish. */
    public static final String SCOPE_CLAIM = SignedTokenMinter.SCOPE_CLAIM;

    /** 32 bytes, because this is the value standing between a stolen file and a month of access. */
    private static final int REFRESH_TOKEN_BYTES = 32;

    /**
     * How stale a "last used" stamp may get. ⚠️ Not zero: every protocol call would otherwise be a write,
     * and a tool call is a read of the product rather than of this table.
     */
    private static final Duration USAGE_STAMP_INTERVAL = Duration.ofMinutes(5);

    private static final SecureRandom SECRETS = new SecureRandom();

    private final AgentDirectory             agents;
    private final AgentConnections           connections;
    private final ProtocolTokenMinter        minter;
    private final McpAuthorizationProperties properties;

    public ProtocolCredentials(
            AgentDirectory agents,
            AgentConnections connections,
            ProtocolTokenMinter minter,
            McpAuthorizationProperties properties) {

        this.agents      = agents;
        this.connections = connections;
        this.minter      = minter;
        this.properties  = properties;
    }

    /**
     * A credential pair, as the token endpoint answers it.
     *
     * @param refreshToken ⚠️ returned once and never recoverable — only its digest is stored
     */
    public record IssuedCredential(String accessToken, String refreshToken, long expiresIn) {
    }

    /** An agent and the connection a call arrived over — the pair every check here needs. */
    public record ActingParty(Agent agent, AgentConnection connection) {
    }

    /**
     * Records a new connection against the agent a person chose, and answers with the credential.
     *
     * <p>⚠️ <strong>Standing is checked here, at redemption, not only at approval.</strong> Those are two
     * requests minutes apart, and the only party whose standing matters is the one that actually turned
     * up — an agent switched off in between gets a refusal rather than a credential.
     */
    @Transactional
    public IssuedCredential issueFor(Agent agent, String clientName, String clientId) {
        AgentAdmission.refusalFor(agent, null, Instant.now()).ifPresent(refusal -> {
            throw new McpAuthorizationException(refusal);
        });

        String refreshToken = newSecret();

        AgentConnection connection = connections.open(
                agent.id(), clientName, clientId,
                refreshToken, Instant.now().plus(properties.getRefreshTokenLifetime()));

        LOGGER.info("Opened protocol connection {} for agent {} ('{}')",
                connection.id(), agent.id(), agent.name());

        return issued(agent, connection, refreshToken);
    }

    /**
     * Renews a connection, replacing the refresh token as it goes.
     *
     * <p>⚠️ <strong>Rotation, and the renewal window slides.</strong> A refresh token that stayed the same
     * would be a long-lived secret travelling on every renewal; a window that did not slide would end a
     * connection somebody uses daily, on a date nobody chose. The consequence to know is that a refresh
     * token spent twice fails the second time — which is what makes a stolen one visible.
     */
    @Transactional
    public IssuedCredential renew(String presentedRefreshToken) {
        if (presentedRefreshToken == null || presentedRefreshToken.isBlank()) {
            throw new McpAuthorizationException("A refresh_token is required to renew a credential.");
        }

        AgentConnection presented = connections.byRefreshToken(presentedRefreshToken)
                .orElseThrow(() -> new McpAuthorizationException(
                        "This refresh token is unknown or has already been replaced. Authorize again to "
                      + "reconnect."));

        Agent agent = agents.find(presented.agentId()).orElse(null);

        // The same four-part question the protocol filter asks on every call, asked here too: an approval
        // and a renewal are different requests, and a connection ended in between must not hand out a
        // fresh month of access.
        AgentAdmission.refusalFor(agent, presented, Instant.now()).ifPresent(refusal -> {
            throw new McpAuthorizationException(refusal);
        });

        // ⚠️ Read off the AGENT. A renewal arrives with nothing but a refresh token — no session, no
        // caller — so the only party that knows whose credential this is, is the row.
        String replacement = newSecret();

        AgentConnection renewed = connections.rotate(
                presented.id(), replacement, Instant.now().plus(properties.getRefreshTokenLifetime()));

        return issued(agent, renewed, replacement);
    }

    /**
     * Whether a call presenting these two identifiers may proceed, and why not where it may not.
     *
     * <p>⚠️ <strong>Four rows, four sentences, one decision.</strong> The agent can be gone, the agent can
     * be switched off, the connection can be revoked, and the connection can have expired — and {@link
     * AgentAdmission} is where those are decided so that every product refuses identically. Answering
     * with the sentence rather than an exception is what lets a protocol filter render an OAuth error
     * while a tool call renders a refusal.
     */
    @Transactional(readOnly = true)
    public Optional<String> admit(String agentId, String connectionId) {
        Agent           agent      = agents.find(agentId).orElse(null);
        AgentConnection connection = connections.find(connectionId).orElse(null);

        if (connection == null) {
            return Optional.of("The connection this credential belongs to no longer exists. Authorize "
                             + "the client again to reconnect.");
        }

        return AgentAdmission.refusalFor(agent, connection, Instant.now());
    }

    /** The agent and connection behind a call, for whoever needs the rows rather than a verdict. */
    @Transactional(readOnly = true)
    public Optional<ActingParty> actingParty(String agentId, String connectionId) {
        return agents.find(agentId).flatMap(agent -> connections.find(connectionId)
                .map(connection -> new ActingParty(agent, connection)));
    }

    /**
     * Every connection a person holds, across all of their agents, newest first.
     *
     * <p>Paired with its agent rather than listed bare: a connection on its own cannot answer <em>what is
     * this</em>, and a screen offering a disconnect button with no reason to press one is a screen nobody
     * can act on.
     */
    @Transactional(readOnly = true)
    public List<ActingParty> connectionsOf(String ownerReference) {
        return pairedWithTheirAgents(agents.ownedBy(ownerReference));
    }

    /**
     * Every connection in the installation, newest first — what an administrator sees.
     *
     * <p>⚠️ <strong>Its own method rather than {@link #connectionsOf} with a null owner</strong>, and the
     * difference is the whole safety of the pair. One of them is answerable to whoever is asking and the
     * other is not; a single method dropping its scope on a falsy argument would be one forgotten null
     * away from handing every connection in the installation to anybody who can sign in. The route above
     * is where the permission lives, and it can only be right if the two questions are two methods.
     *
     * <p>⚠️ <strong>The limit is the directory's, and it is a real ceiling rather than paging.</strong>
     * {@code AgentDirectory.all} takes one, so an installation past it shows the first N and says nothing
     * — worth replacing with paging the first time anybody has that many agents, and worth not pretending
     * otherwise until then.
     */
    @Transactional(readOnly = true)
    public List<ActingParty> everyConnection(int limit) {
        return pairedWithTheirAgents(agents.all(limit));
    }

    /**
     * Ends one client.
     *
     * <p>Scoped to the owner on purpose: a connection identifier is not a secret, and the person who
     * approved a connection is the person who may end it. ⚠️ The ownership check walks connection → agent
     * → {@code ownerReference}, because the connection does not know whose it is — that is the agent's
     * fact, and storing it twice is how the two drift.
     */
    @Transactional
    public boolean revoke(String connectionId, String ownerReference) {
        return endIf(connectionId, connection -> isOwnedBy(connection, ownerReference));
    }

    /**
     * Ends one client, whoever approved it — the administrator's half of {@link #revoke}.
     *
     * <p>⚠️ <strong>No ownership check, and that is the point of it existing.</strong> A connection is
     * ended by the person who approved it or by somebody who administers the installation, and the second
     * cannot be expressed by the first: an administrator is not the owner of anybody's agent. Which is
     * exactly why this is a separate method behind a separate route behind a permission, rather than a
     * flag on the one above.
     */
    @Transactional
    public boolean revokeAnywhere(String connectionId) {
        return endIf(connectionId, connection -> true);
    }

    /**
     * Records that a connection was used, at most once every {@link #USAGE_STAMP_INTERVAL}.
     *
     * <p>The agent is stamped too, on the same schedule and for a different question: the connection's
     * stamp answers <em>which of these clients can I safely disconnect</em>, and the agent's answers
     * <em>is this one still in use at all</em>.
     */
    @Transactional
    public void noteUsage(String agentId, String connectionId) {
        connections.find(connectionId)
                .filter(connection -> isStampStale(connection.lastUsedAt()))
                .ifPresent(connection -> {
                    Instant now = Instant.now();

                    connections.stampUsed(connectionId, now);
                    agents.stampActive(agentId, now);
                });
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    /**
     * ⚠️ Answers {@code false} for a connection that is not there or not the caller's, and {@code true}
     * for one already ended. The caller turns the first into whatever "not found" means in its product —
     * this module has no exception to throw that a product would want — while the second is a request
     * that should succeed quietly: somebody clicking a stale row has done nothing wrong.
     */
    private boolean endIf(String connectionId, java.util.function.Predicate<AgentConnection> reachable) {
        Optional<AgentConnection> connection = connections.find(connectionId).filter(reachable);

        if (connection.isEmpty()) {
            return false;
        }

        if (connection.get().isRevoked()) {
            return true;
        }

        connections.revoke(connectionId);
        LOGGER.info("Revoked protocol connection {}", connectionId);

        return true;
    }

    private List<ActingParty> pairedWithTheirAgents(List<Agent> chosen) {
        return chosen.stream()
                .flatMap(agent -> connections.of(agent.id()).stream()
                        .map(connection -> new ActingParty(agent, connection)))
                .sorted(Comparator.comparing(
                        (ActingParty acting) -> acting.connection().issuedAt()).reversed())
                .toList();
    }

    private boolean isOwnedBy(AgentConnection connection, String ownerReference) {
        return agents.find(connection.agentId())
                .map(agent -> ownerReference.equals(agent.ownerReference()))
                .orElse(false);
    }

    /**
     * ⚠️ The lifetime comes back from the minter rather than from the properties, and the difference is
     * not cosmetic: a product minting its own token decides how long it stands, and reading the library's
     * property instead would tell a client a number the token does not honour — a client that renews too
     * late, or needlessly early, with nothing in either case saying why.
     */
    private IssuedCredential issued(Agent agent, AgentConnection connection, String refreshToken) {
        ProtocolTokenMinter.MintedToken minted = minter.mint(agent, connection);

        return new IssuedCredential(minted.accessToken(), refreshToken, minted.expiresInSeconds());
    }

    /**
     * ⚠️ The refresh token in the clear, returned once and never recoverable — {@code AgentConnections}
     * stores only its digest. Which is why it is generated here rather than by a caller: a value this
     * short-lived in memory and this long-lived in effect should have exactly one place it comes from.
     */
    private static String newSecret() {
        byte[] bytes = new byte[REFRESH_TOKEN_BYTES];

        SECRETS.nextBytes(bytes);

        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private boolean isStampStale(Instant lastUsedAt) {
        return lastUsedAt == null || lastUsedAt.isBefore(Instant.now().minus(USAGE_STAMP_INTERVAL));
    }
}
