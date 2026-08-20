package org.jmouse.ai.mcp.authorization.server;

import org.jmouse.ai.agent.Agent;
import org.jmouse.ai.agent.AgentConnection;
import org.jmouse.ai.mcp.authorization.AuthorizationRoutes;
import org.jmouse.ai.mcp.authorization.McpAuthorizationException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Instant;
import java.util.List;

/**
 * A credential the product signs itself, confined to the protocol endpoint by its signature.
 *
 * <h2>⚠️ Why a product signs this rather than its identity server minting it</h2>
 *
 * <p>A central identity server mints every token a browser uses. What it cannot mint is a credential
 * <strong>confined to one endpoint</strong>: an audience names a service, not a route, so a token good
 * for the protocol would be equally good for every REST call the product serves. So this one is signed
 * HS256 with a secret only the product holds, while every other route validates RS256 against the
 * identity server's JWKS. Neither decoder can be made to accept the other's token — <em>"this credential
 * works nowhere else"</em> is a signature that does not verify, rather than a check somebody could
 * forget to write.
 *
 * <h2>⚠️ The credential acts as the PERSON, and names an agent beside them</h2>
 *
 * <ul>
 *   <li>{@code sub} is the <strong>owner's</strong> subject, so every authority converter resolves a
 *       protocol caller exactly as it resolves that person's browser. A tool sees what they see.
 *   <li>{@code aid} is the agent — what an author column points at, and what a person switches off.
 *   <li>{@code cid} is the connection, which is what makes a self-contained token revocable at all.
 * </ul>
 */
public class SignedTokenMinter implements ProtocolTokenMinter {

    /** Names the connection an access token was issued against, so revocation can reach it. */
    public static final String CREDENTIAL_CLAIM = "cid";

    /** And the agent it acts as, so attribution does not cost a second lookup. */
    public static final String AGENT_CLAIM = "aid";

    /** What the credential is for, in the one word the discovery documents also publish. */
    public static final String SCOPE_CLAIM = "scope";

    /** ⚠️ Read by products that refresh a cached profile from whatever token arrives. */
    private static final String NAME_CLAIM  = "name";
    private static final String EMAIL_CLAIM = "email";

    /** Which client holds it, so a log line and a connections screen can name one. */
    private static final String CLIENT_CLAIM = "client";

    private final CredentialOwners           owners;
    private final JwtEncoder                 encoder;
    private final McpAuthorizationProperties properties;

    public SignedTokenMinter(
            CredentialOwners owners, JwtEncoder encoder, McpAuthorizationProperties properties) {

        this.owners     = owners;
        this.encoder    = encoder;
        this.properties = properties;

        // ⚠️ Refused at construction rather than at the first issue, and refused HERE rather than on the
        // credential service: a product minting its own token — Innoventa does — has no audience of ours
        // to state, and failing its startup over a property it does not use would be a bug of our making.
        if (properties.getAudience() == null || properties.getAudience().isBlank()) {
            throw new IllegalStateException(
                    McpAuthorizationProperties.PREFIX + ".audience must be set — a signed protocol "
                  + "credential with no audience is one no decoder can be made to refuse.");
        }
    }

    @Override
    public MintedToken mint(Agent agent, AgentConnection connection) {
        CredentialOwners.CredentialOwner owner = owners.find(agent.ownerReference())
                .orElseThrow(() -> new McpAuthorizationException(
                        "The person this agent acts for no longer exists. Nothing was changed."));

        Instant now = Instant.now();

        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(properties.getResourceUrl())
                .audience(List.of(properties.getAudience()))
                .subject(owner.subject())
                .issuedAt(now)
                .expiresAt(now.plus(properties.getAccessTokenLifetime()))
                .claim(SCOPE_CLAIM,      AuthorizationRoutes.SCOPE)
                .claim(CREDENTIAL_CLAIM, connection.id())
                .claim(AGENT_CLAIM,      agent.id())
                .claim(CLIENT_CLAIM,     connection.clientName());

        // ⚠️ Skipped when absent rather than passed through: JwtClaimsSet refuses a null value outright,
        // and a person with no email is ordinary — a subject is not always an address.
        claimIfPresent(claims, NAME_CLAIM,  owner.displayName());
        claimIfPresent(claims, EMAIL_CLAIM, owner.email());

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();

        return new MintedToken(
                encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue(),
                properties.getAccessTokenLifetime().toSeconds());
    }

    private static void claimIfPresent(JwtClaimsSet.Builder claims, String name, String value) {
        if (value != null && !value.isBlank()) {
            claims.claim(name, value);
        }
    }
}
