package org.jmouse.access.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.jmouse.access.AccessTarget;
import org.jmouse.access.ScopeKind;
import org.jmouse.access.ScopeReference;
import org.jmouse.access.spi.AccessTargetResolver;
import org.jmouse.access.spi.ScopeHierarchy;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A resolver that answers by <strong>projecting two columns</strong>, never by loading a row.
 *
 * <p>Most of the answer is the same shape everywhere: an owner identifier and a place
 * identifier, read off one table. Writing that fourteen times would be fourteen chances to load an
 * aggregate by accident on the security path — which is the one path where "it is only one extra
 * query" is charged on every request rather than on one screen.
 *
 * <p>A subclass says which entity, and which two paths. Where a resource has no owner (shared
 * furniture nobody owns) or lives in no place (a personal row), it passes {@code null} and the target
 * simply does not name one.
 *
 * <p>Wider places are filled in from the narrow one by {@link ScopeHierarchy}, so that a grant
 * at a wider place covers rows in the narrower ones it contains, without every feature having
 * to know that wider places exist.
 *
 * @param <T> the resource type
 */
public abstract class ProjectedAccessTargetResolver<T> implements AccessTargetResolver<T> {

    // @PersistenceContext rather than the constructor injection this codebase otherwise insists on,
    // and the exception is deliberate. This class is abstract with twelve subclasses; threading an
    // EntityManager through twelve constructors to satisfy a convention aimed at COLLABORATOR
    // injection would make twelve files worse to make one conform. The subclasses do inject their
    // one real collaborator through their constructors, which is what the convention is protecting.
    @PersistenceContext
    private EntityManager entityManager;

    private final ScopeHierarchy scopeHierarchy;

    protected ProjectedAccessTargetResolver(ScopeHierarchy scopeHierarchy) {
        this.scopeHierarchy = scopeHierarchy;
    }

    /** The JPQL path to the owner's identifier, or null where nobody owns one of these. */
    protected abstract String ownerPath();

    /** The JPQL path to the identifier of the place this resource lives in, or null where it lives in none. */
    protected abstract String placePath();

    /**
     * ⚠️ <strong>{@code getResultList}, never {@code getResultStream}, and that is not a style
     * choice.</strong>
     *
     * <p>Hibernate backs a result stream with a scrollable {@code ResultSet} that stays open for lazy
     * iteration. This resolver runs from {@code AccessAuthorizationInterceptor} — <em>before</em> the
     * handler it guards, and therefore <em>outside</em> the handler's transaction. With no transaction
     * to hold it, the connection goes back to the pool the moment the query returns, and the first
     * {@code next()} lands on a closed {@code ResultSet}:
     *
     * <pre>Error advancing (next) ResultSet position [Operation not allowed after ResultSet closed]</pre>
     *
     * <p>Which reaches the caller as a 500 on every route that names a row. Annotating this class
     * {@code @Transactional} would also work and would be the wrong fix: it would open a transaction
     * per authorization check on every request, to hold open a cursor over one row.
     */
    @Override
    public Optional<AccessTarget> resolve(String resourceId) {
        if (resourceId == null || resourceId.isBlank()) {
            return Optional.empty();
        }

        return entityManager
                .createQuery(projection() + " WHERE resource.id = :id", Object[].class)
                .setParameter("id", resourceId)
                .setMaxResults(1)
                .getResultList()
                .stream()
                .findFirst()
                .map(this::toTarget);
    }

    @Override
    public Map<String, AccessTarget> resolveAll(List<String> resourceIds) {
        if (resourceIds == null || resourceIds.isEmpty()) {
            return Map.of();
        }

        // One query for the page. A listing that resolved per row would put the N+1 back on exactly
        // the paths that already cost the most. Materialised rather than streamed for the reason
        // given on resolve() above — there is no transaction here to keep a cursor open.
        return entityManager
                .createQuery(projection() + " WHERE resource.id IN :ids", Object[].class)
                .setParameter("ids", resourceIds.stream().distinct().toList())
                .getResultList()
                .stream()
                .collect(Collectors.toMap(row -> (String) row[0], this::toTarget, (first, second) -> first));
    }

    private String projection() {
        return "SELECT resource.id, " + projectedColumn(ownerPath()) + ", " + projectedColumn(placePath())
               + " FROM " + resourceType().getSimpleName() + " resource";
    }

    /** The SELECT item for a path, or the NULL literal where this resource has no such column. */
    private static String projectedColumn(String path) {
        return path == null ? "NULL" : "resource." + path;
    }

    /**
     * The two columns, plus every wider place the narrower one sits inside.
     *
     * <p>A feature answers about the place it knows — a form knows which place it is in — and the
     * containment above that is somebody else's business. {@link ScopeHierarchy} supplies it, so a
     * grant at a wider place covers the rows in the narrower ones without any feature learning that
     * those wider places exist.
     */
    private AccessTarget toTarget(Object[] row) {
        String ownerId = (String) row[1];
        String placeId = (String) row[2];

        AccessTarget target = AccessTarget.installation().withOwner(ownerId);

        if (placeId == null || placeId.isBlank()) {
            return target;
        }

        target = target.at(placeOf(), placeId);

        for (ScopeReference wider : scopeHierarchy.containing(ScopeReference.of(placeOf(), placeId))) {
            target = target.at(wider.type(), wider.id());
        }

        return target;
    }

    /**
     * Which kind of place {@link #placePath()} points at.
     *
     * <p>Abstract rather than defaulted, because a library has no honest default: the whole point of
     * the scope catalogue is that a product names its own floors, so guessing one here would be the
     * one place this module knew a place by name. A product with a single floor answers it once in a
     * base class of its own and its resolvers never mention it again.
     */
    protected abstract ScopeKind placeOf();
}
