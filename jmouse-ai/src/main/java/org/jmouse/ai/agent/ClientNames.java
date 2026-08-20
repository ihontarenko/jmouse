package org.jmouse.ai.agent;

/**
 * What a client is called, and the one case where that is not a name at all.
 *
 * <h2>⚠️ Why the placeholder lives here rather than beside the registry that produces it</h2>
 *
 * <p>A client names itself when it registers, and a server that has forgotten — or never saw — that
 * registration has to show <em>something</em>. The something is {@link #UNNAMED}, and it is produced by
 * the authorization module. But it is <strong>consumed here</strong>, because the name does not stop at a
 * screen: it is copied onto an {@link AgentConnection} and, on first connection, becomes the
 * {@link Agent}'s own name.
 *
 * <p>Which makes the placeholder a value this module has to be able to recognise. Matching an agent by
 * name is right for a client that named itself and catastrophic for one that did not: two clients that
 * both came through unnamed are two clients, and treating them as one gives a laptop and a desktop a
 * single persona — one permission set, one switch, and disconnecting either taking the other's identity
 * with it.
 *
 * <p>⚠️ So the constant is defined here and the authorization module points at it. The alternative —
 * each side spelling the same sentence out — is a rule that holds until somebody improves the wording on
 * one of them, and then silently stops holding with no test able to see it.
 */
public final class ClientNames {

    /** What a client is shown as, and recorded as, when it has told us nothing usable. */
    public static final String UNNAMED = "An unnamed client";

    /** Enough of an identifier to tell two of them apart, and not enough to look like one. */
    private static final int DISCRIMINATOR_LENGTH = 6;

    private ClientNames() {
    }

    /**
     * Whether this name identifies a client, as opposed to standing in for one that has no name.
     *
     * <p>⚠️ The question worth asking before matching anything <em>by</em> a name. A blank one and
     * {@link #UNNAMED} are the same answer — nobody said — and the difference between them is only which
     * layer noticed first.
     */
    public static boolean identifies(String clientName) {
        return clientName != null && !clientName.isBlank() && !UNNAMED.equals(clientName);
    }

    /**
     * A name that tells one nameless client from another, for the row that has to hold a distinct one.
     *
     * <h2>⚠️ Because a persona's name is unique per owner, and {@link #UNNAMED} is not a name</h2>
     *
     * <p>An owner may have one agent by any given name. Two clients that both arrived nameless would
     * therefore ask for the same one, and the second would be <em>refused a credential</em> — over a
     * label, at the last step, after a person had already approved the screen. Which is a confusing way
     * to be told that something unrelated already exists.
     *
     * <p>So the identifier the server itself issued is appended. Only its tail: the whole thing is long,
     * opaque and helps nobody read the row, whereas a few characters are enough to see that these are two
     * clients rather than one.
     *
     * <p>⚠️ Applies to the <strong>unnamed case alone</strong>. A client that named itself keeps exactly
     * what it said, because reconnecting matches on that name.
     */
    public static String distinguish(String clientName, String clientId) {
        if (identifies(clientName)) {
            return clientName;
        }

        if (clientId == null || clientId.isBlank()) {
            return UNNAMED;
        }

        return UNNAMED + " · " + clientId.substring(Math.max(0, clientId.length() - DISCRIMINATOR_LENGTH));
    }
}
