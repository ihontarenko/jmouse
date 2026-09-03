package org.jmouse.access.spi;

import org.jmouse.access.AccessTarget;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Where one feature's rows live, and who owns them — declared by the feature, asked by the engine.
 *
 * <p>The seam that lets ownership stop being a mechanism. Six guards used to answer "is this yours"
 * in six shapes — {@code SecurityPermissionService.requireOwnerAccess}, {@code FileAccessGuard},
 * {@code ProjectAccessGuard}, {@code EmbedGuard}, {@code VisibleForms.PREDICATE} and
 * {@code EntryVisibility} — each with its own idea of what an owner is and, in four cases, no idea at
 * all that a service sub-account owns nothing. Once a resource can say where it lives, "owner or
 * override" is one implementation in the engine.
 *
 * <p><strong>The direction is the established one</strong> (ADR-0010): a lower level declares a seam,
 * a higher level implements it in its own package — the same shape as {@code platform.audit.spi},
 * {@code platform.location.service.spi} and {@code engine.form.service.spi.EntryValuesListener}.
 * {@code platform.access} never imports a feature; each feature contributes a bean.
 *
 * <p><strong>A resolver is a lookup, not a load.</strong> It answers three identifiers. It must not
 * fetch an aggregate, and it must be cheap enough to run <em>before</em> the thing it describes is
 * read — which is the whole point: the engine decides whether the caller may have it, and only then
 * does anybody go and get it.
 *
 * @param <T> the resource type this resolver speaks for
 */
public interface AccessTargetResolver<T> {

    /** The type this resolver answers about. Two resolvers for one type is a startup failure. */
    Class<T> resourceType();

    /**
     * The word a policy writes for this resource — {@code through form}, {@code kind: "form"}.
     *
     * <p>The default reads {@link AccessResourceName} off the type, which is where the name belongs for
     * anything this product owns: it sits beside the class it names, and a reader of that class can see
     * that the word is load-bearing.
     *
     * <p>⚠️ <strong>Override it for a type from a library that does not know about access control.</strong>
     * {@code ManagedFile} and {@code StorageDirectory} come from {@code jmouse-files}, which has no
     * dependency on this module and must not grow one — a storage library that cannot be used without an
     * authorization library is the wrong shape. Their resolver lives in the product, so the name is
     * declared there:
     *
     * <pre>
     * &#64;Override
     * public String resourceName() {
     *     return "file";
     * }
     * </pre>
     *
     * <p>⚠️ <strong>What is never allowed is deriving it.</strong> Both forms are a written-down word; the
     * only difference is which file it is written in. A name computed from {@code getSimpleName()} is a
     * contract nobody can see and a rename silently breaks — see {@link AccessResourceName}.
     */
    default String resourceName() {
        return AccessResourceNames.of(resourceType());
    }

    /**
     * Where this row lives and who owns it, or nothing where there is no such row.
     *
     * <p>Empty means "no such row", which the engine reads as {@code NOT_FOUND_OR_HIDDEN} rather than
     * as an open door — an unknown identifier must never resolve to an unscoped target, because an
     * unscoped target passes every axis that is about a place.
     */
    Optional<AccessTarget> resolve(String resourceId);

    /**
     * The same answer for a page of rows, in one query.
     *
     * <p>Overridden wherever a listing needs many — a page of twenty-five rows resolved one at a time
     * is the N+1 spec risk 1 names, and it appears on exactly the paths that are already the slowest.
     * The default is honest rather than fast, and is fine for a resource nothing lists.
     */
    default Map<String, AccessTarget> resolveAll(List<String> resourceIds) {
        return resourceIds.stream()
                .distinct()
                .map(resourceId -> resolve(resourceId).map(target -> Map.entry(resourceId, target)))
                .flatMap(Optional::stream)
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
