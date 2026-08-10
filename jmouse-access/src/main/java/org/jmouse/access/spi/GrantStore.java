package org.jmouse.access.spi;

import org.jmouse.access.ScopeReference;

import java.util.List;

/**
 * Where grants are kept — the seam between the engine and whatever a product stores them in.
 *
 * <p>The engine resolves; it does not read tables. Until this interface existed it did both:
 * {@code EffectivePermissionsResolver} and {@code VisibilityScopeResolver} injected two Spring Data
 * repositories and walked JPA entities, which meant the authorization model could not be lifted out of
 * a product that happened to store roles one particular way. Everything above this interface now
 * speaks {@link ScopeReference}s and permission strings, and a schema is on the other side.
 *
 * <h2>Two questions, and why they are not one</h2>
 *
 * <p><strong>Covering</strong> is what a <em>check</em> asks: it has a row, so it knows the chain of
 * scopes that could possibly matter and wants only the grants sitting on it. <strong>Held by</strong>
 * is what a <em>filter</em> asks: it has no row yet, so it cannot name a chain, and it needs every
 * grant the subject has anywhere in order to work out which rows to allow. Serving the second from the
 * first is impossible and serving the first from the second is the N+1 the whole design avoids.
 *
 * <p><strong>An implementation must return only exact matches.</strong> A typical covering query
 * matches scope types and identifiers as two independent lists, so that a chain of four costs one
 * round trip rather than four; the widened result that produces is narrowed back inside the adapter.
 * An engine that had to know a store's queries were approximate would be an engine that knew the
 * store.
 *
 * <p>Deliberately read-only. Handing out a grant is a product's own business — its screens, its
 * audit trail, its rules about who may — and an interface that could also write would invite the
 * engine to.
 */
public interface GrantStore {

    /** Every role this subject holds at one of these scopes, and nothing at any other. */
    List<RoleGrant> rolesCovering(String subjectId, List<ScopeReference> chain);

    /** Every personal allow or deny for this subject at one of these scopes, and nothing at any other. */
    List<DirectGrant> directCovering(String subjectId, List<ScopeReference> chain);

    /** Every role this subject holds, anywhere — what a listing filter has to start from. */
    List<RoleGrant> rolesHeldBy(String subjectId);

    /** Every personal allow or deny for this subject, anywhere. */
    List<DirectGrant> directHeldBy(String subjectId);
}
