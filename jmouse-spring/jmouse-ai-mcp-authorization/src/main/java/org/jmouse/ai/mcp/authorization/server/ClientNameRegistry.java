package org.jmouse.ai.mcp.authorization.server;

import org.jmouse.ai.agent.ClientNames;

import java.net.URI;
import java.util.List;

/**
 * The names clients register themselves under, kept only so the consent screen can say who is asking.
 *
 * <p>⚠️ <strong>It is worth being plain about how little a registration means, because the
 * specification's word for it suggests more.</strong> An identifier issued this way authenticates
 * nobody: anyone may register, no secret is issued, and nothing about the identifier is checked when it
 * comes back. What actually stands between a client and a credential is unchanged — a loopback-only
 * redirect address, proof of possession, and a person approving a screen that names both the client and
 * where the code will be sent.
 *
 * <p>⚠️ <strong>Which is why {@link #nameOf} answers rather than refuses</strong> — and why
 * {@link #recognises} exists beside it. A name is a label and degrading one is harmless, so the method
 * that returns a name never turns anybody away. Whether the registration is <em>known at all</em> is a
 * different question with a different answer, asked once, at the top of the flow: see
 * {@link McpProtocolEndpoints}.
 *
 * <p>⚠️ <strong>A registration is kept alive by being used.</strong> The expiry is a sliding window
 * rather than a deadline, because the client this is about does not re-register: it caches the identifier
 * it was issued and presents the same one for as long as it lives. Sweeping on the original date would
 * therefore delete the row of the client using it <em>most</em>, and the first symptom would be a working
 * connection quietly renaming itself to {@link #UNNAMED} — permanently, since the name is copied onto a
 * connection and, in one product, onto an agent.
 *
 * <p>An interface because durability is a product's call and not a requirement: a map that forgets on
 * restart and a Redis key that does not are both correct, and the second is only worth the dependency to
 * somebody who already has it.
 */
public interface ClientNameRegistry {

    /**
     * What a client is shown as when it has told us nothing usable.
     *
     * <p>⚠️ Defined in {@link ClientNames} rather than here, because the placeholder outlives the screen:
     * it is copied onto a connection and can become an agent's name, so the module that owns those rows
     * has to be able to recognise it and refuse to match on it.
     */
    String UNNAMED = ClientNames.UNNAMED;

    /**
     * Records a client's name against a fresh identifier, and returns the identifier.
     *
     * @param clientName what the client called itself. ⚠️ Pass it through
     *                   {@link #describe(String, List)} first where the redirect addresses are known —
     *                   what the server can see about a client is worth more on a consent screen than
     *                   what it says about itself
     */
    String register(String clientName);

    /**
     * How a client should be named on the consent screen. Always answers something.
     *
     * <p>⚠️ Finding a live registration <strong>slides its expiry</strong>, so a client in regular use
     * never loses its name to the sweep — see the class note.
     */
    String nameOf(String clientId);

    /**
     * Whether this installation issued this identifier and still holds it.
     *
     * <p>⚠️ <strong>The one question in this interface that a caller is expected to act on.</strong>
     * {@link #nameOf} deliberately answers for an identifier it has never seen, because a missing label is
     * not worth refusing over. But a client presenting an identifier nobody here issued cannot be named,
     * cannot be told apart from any other such client, and — since the name reaches durable rows — will
     * bake that into whatever it connects as. The flow answers that with {@code invalid_client}, which is
     * the one refusal a conformant client knows how to fix by registering again.
     */
    boolean recognises(String clientId);

    /** A client names itself, so what it says is stored as a claim, never trusted as a fact. */
    static String describe(String clientName) {
        if (!ClientNames.identifies(clientName)) {
            return UNNAMED;
        }

        return clientName.trim();
    }

    /**
     * The name a client claimed, with what the server can see about it appended.
     *
     * <h2>⚠️ A suffix, and never an identity</h2>
     *
     * <p>The one thing here that is <strong>not</strong> the client's word is where it asked to be sent
     * back to. A loopback address means the client is running on the machine whose browser is about to
     * approve it; anything else means it is not. That is worth saying on a screen where somebody is
     * deciding whether to hand out a credential, and worth carrying onto the connection row so a list of
     * three "Claude Code" entries can be told apart.
     *
     * <p>⚠️ It is appended rather than substituted, and it never replaces a missing name. An unnamed
     * client stays unnamed — <em>"this machine"</em> is not a name, and a row reading only that would
     * look like an identity the server had verified. What it verified is one address.
     *
     * <p>⚠️ The port is deliberately left out. A client picks a fresh one per authorization, so
     * including it would make the same client on the same machine look like a different one every time —
     * which is exactly the confusion this is meant to reduce.
     */
    static String describe(String clientName, List<String> redirectUris) {
        String claimed = describe(clientName);

        if (UNNAMED.equals(claimed) || redirectUris == null || redirectUris.isEmpty()) {
            return claimed;
        }

        return redirectUris.stream().allMatch(ClientNameRegistry::isLoopback)
                ? claimed + " · this machine"
                : claimed;
    }

    private static boolean isLoopback(String redirectUri) {
        try {
            String host = URI.create(redirectUri).getHost();

            return "localhost".equals(host) || "127.0.0.1".equals(host) || "::1".equals(host);
        } catch (RuntimeException malformed) {
            // ⚠️ Not this method's business. The redirect policy refuses an unusable address, loudly,
            // before anything gets here; a label must not be the thing that decides an address is bad.
            return false;
        }
    }
}
