package org.jmouse.ai.mcp.authorization.server;

import java.util.Optional;

/**
 * Who a protocol credential acts as.
 *
 * <h2>⚠️ This is the only thing about issuing a credential that differs between products</h2>
 *
 * <p>Everything else — rotating a refresh token, the four ways a call is refused, the claims a token
 * carries, how a connection is listed and ended — was written three times, identically, in three
 * products. The one genuine difference is the row on the other end of {@link
 * org.jmouse.ai.agent.Agent#ownerReference()}: one product calls it a member, another an identity user,
 * a third a security user. So that is the seam, and it is deliberately four strings wide.
 *
 * <h2>⚠️ It answers about the OWNER, never about the agent</h2>
 *
 * <p>The reference passed in is {@code Agent.ownerReference()} and nothing else. A product tempted to
 * resolve it through its own parent-child mirror — {@code members.parent_id},
 * {@code security_users.parent_id} — is reading the record-keeping column to decide what a credential
 * may do, which is the read every one of those products has an ADR forbidding. The authoritative field
 * is the one handed to this method.
 *
 * <p>Answering {@link Optional#empty()} means the person is gone, and the caller turns that into a
 * refusal rather than a credential — an agent outliving its owner must not keep acting for them.
 */
@FunctionalInterface
public interface CredentialOwners {

    /**
     * The person an agent acts for, by the reference the agent row carries.
     *
     * @param ownerReference {@code Agent.ownerReference()}, never a parent-child mirror
     */
    Optional<CredentialOwner> find(String ownerReference);

    /**
     * What a token needs to say about the person it acts as.
     *
     * <h2>⚠️ Why the name and email are here at all</h2>
     *
     * <p>They are not identity — {@code subject} is. They are on the record because every product that
     * built this refreshes a cached profile from whatever token arrives, so a token carrying only a
     * subject quietly renames the person to their own identifier. Both are optional: an installation's
     * subject is not always an address, and a blank one is skipped rather than written as empty.
     *
     * @param subject     what the {@code sub} claim says — the OWNER's, so a tool call authorizes exactly
     *                    as that person's browser does
     * @param displayName may be blank
     * @param email       may be blank
     */
    record CredentialOwner(String subject, String displayName, String email) {
    }
}
