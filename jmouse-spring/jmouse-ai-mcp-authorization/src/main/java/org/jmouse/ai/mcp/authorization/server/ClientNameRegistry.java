package org.jmouse.ai.mcp.authorization.server;

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
 * <p>⚠️ <strong>Which is why {@link #nameOf} answers rather than refuses.</strong> An identifier this
 * installation has never seen is a client it cannot name, not a client it should turn away — refusing
 * would strand a working client over a cache eviction or a restart, and would be pretending the
 * identifier means something it does not.
 *
 * <p>An interface because durability is a product's call and not a requirement: a map that forgets on
 * restart and a Redis key that does not are both correct, and the second is only worth the dependency to
 * somebody who already has it.
 */
public interface ClientNameRegistry {

    /** What a client is shown as when it has told us nothing usable. */
    String UNNAMED = "An unnamed client";

    /** Records a client's name against a fresh identifier, and returns the identifier. */
    String register(String clientName);

    /** How a client should be named on the consent screen. Always answers something. */
    String nameOf(String clientId);

    /** A client names itself, so what it says is stored as a claim, never trusted as a fact. */
    static String describe(String clientName) {
        if (clientName == null || clientName.isBlank()) {
            return UNNAMED;
        }

        return clientName.trim();
    }
}
