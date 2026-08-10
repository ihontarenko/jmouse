package org.jmouse.access.spi;

import org.jmouse.access.AccessTarget;

import java.util.Optional;

/**
 * What a share link grants, asked of the feature that mints them.
 *
 * <p>The third path that used to go around the whole authorization system rather than through it. An
 * unauthenticated visitor holding a link was not a subject with a small permission set — they were a
 * route that skipped the question, and the only thing standing between them and the resource was a
 * token lookup written into each public controller separately.
 *
 * <p>Here a share is <strong>an anonymous subject with a derived permission set</strong>: exactly one
 * permission, over exactly the resource the token names, at the scope that resource lives in. The
 * {@code /_/} routes then answer through the same five axes as everything else — which is what makes
 * "this share is for a workspace whose plan no longer includes the module" answerable at all.
 *
 * <p><strong>The direction is the established one</strong> (ADR-0010): {@code platform.access}
 * declares the seam and {@code platform.sharing} implements it. The engine never learns what a token
 * looks like, and the sharing mechanism never learns what an axis is.
 *
 * <p>⚠️ <strong>File bytes deliberately do not come through here.</strong> {@code /_/file/{token}} is
 * unauthenticated by design and is protected by {@code ContentDispositions} — active content is never
 * served inline, because that route is proxied onto the SPA's own origin. Routing bytes through the
 * engine would buy nothing it does not already have and would put a database round trip on every
 * image in every page. The exception is a decision, not an oversight.
 */
public interface ShareGrants {

    /**
     * What this token grants, or nothing where it grants nothing — no such token, revoked, expired.
     *
     * <p>Nothing is a refusal rather than an unscoped pass, for the same reason an unresolvable
     * resource identifier is: an empty target reaches every axis that is about a place.
     */
    Optional<SharedGrant> of(String shareToken);

    /**
     * One permission over one resource, in one place.
     *
     * @param permission what the holder of the link may do — read, and in this mechanism only read
     * @param target     where the shared resource lives, so the ceiling, the entitlement and the
     *                   workspace's own switch all still get their say about it
     */
    record SharedGrant(String permission, AccessTarget target) {
    }
}
