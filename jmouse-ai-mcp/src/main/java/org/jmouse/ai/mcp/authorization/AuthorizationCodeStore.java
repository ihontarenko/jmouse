package org.jmouse.ai.mcp.authorization;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;

/**
 * The seam. ⚠️ <strong>Everything on the account side of the flow is behind it.</strong>
 *
 * <p>This package holds the parts of client authorization that are requirements of the <em>protocol</em>
 * — a challenge method that must be S256, a redirect address that must be loopback, a discovery document
 * that must say where the authorization server is. Those are the same in every installation and belong
 * in a library.
 *
 * <p>Minting a credential is not one of them. It requires knowing what an account is, which accounts may
 * hold a protocol credential at all, what a session is, and how long any of it lasts — every one of
 * which is a product's decision, and a library that had an opinion about them would be a library every
 * adopter fights. So a product implements this, and what it stores against a code is
 * {@link PendingAuthorization#subjectReference()}: an opaque string this library never reads.
 *
 * <p>The store is also where expiry and single use live, because both are storage properties. A code
 * that could be redeemed twice is a code that can be replayed, and nothing above this interface is in a
 * position to prevent it.
 */
public interface AuthorizationCodeStore {

    /**
     * Records an approved authorization and answers with the code that redeems it.
     *
     * <p>⚠️ The code is a bearer credential in a query string. It should be long, random and short-lived,
     * and it must never be written anywhere a query string is logged.
     */
    String issue(PendingAuthorization pending);

    /**
     * Spends a code, if it exists and has not been spent or expired.
     *
     * <p>⚠️ <strong>Consuming, not reading.</strong> A store that returned the authorization and left it
     * in place would allow a replay, and the caller is not in a position to notice.
     */
    Optional<PendingAuthorization> redeem(String code);

    /**
     * One approved authorization, waiting to be exchanged.
     *
     * @param clientId         which client asked
     * @param redirectUri      where its code was sent, already vetted by {@link LoopbackRedirectPolicy}
     * @param challenge        the S256 challenge the redeeming verifier is checked against
     * @param subjectReference whoever approved it, in the product's own vocabulary — <strong>never read
     *                         by this library</strong>
     * @param expiresAt        when the code stops being redeemable
     */
    record PendingAuthorization(
            String  clientId,
            URI     redirectUri,
            String  challenge,
            String  subjectReference,
            Instant expiresAt
    ) {

        public boolean hasExpired(Instant now) {
            return now.isAfter(expiresAt);
        }
    }
}
