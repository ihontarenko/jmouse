package org.jmouse.web.mvc.resource;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Outcome;
import org.jmouse.core.io.Resource;

import java.util.LinkedList;
import java.util.List;

/**
 * 🏷️ Resource resolver that applies {@link VersionStrategy} rules.
 *
 * <p>Supports versioned resource URLs (e.g. {@code /v1/js/app.js})
 * by stripping version prefix, delegating resolution, and wrapping
 * the result into a {@link VersionalResource} with an {@code ETag}.</p>
 */
public class VersionalResourceResolver extends AbstractResourceResolver {

    /** 📋 Registered version strategies. */
    private final List<VersionStrategy> strategies = new LinkedList<>();

    /**
     * 🏗️ Create resolver with a default strategy ({@code /v1/** → v1}).
     */
    public VersionalResourceResolver() {
        this.strategies.add(new FixedVersionStrategy("/v1/**", "v1"));
    }

    /**
     * 🔎 Attempt to resolve a resource with version support.
     *
     * <ul>
     *   <li>Finds a matching {@link VersionStrategy}</li>
     *   <li>Strips version from the request path</li>
     *   <li>Delegates to the next resolver with a clean path</li>
     *   <li>Wraps resolved resource in {@link VersionalResource}</li>
     *   <li>Throws {@link ResourceNotFoundException} if not found</li>
     * </ul>
     *
     * @param context current HTTP request
     * @param query   resource lookup query
     * @param next    next resolver in the chain
     * @return outcome with versioned resource, or continue if no strategy applies
     */
    @Override
    public Outcome<Resource> handle(
            HttpServletRequest context,
            ResourceQuery query,
            Chain<HttpServletRequest, ResourceQuery, Resource> next
    ) {
        ResourceQuery   newQuery    = query;
        String          requestPath = query.path();
        VersionStrategy strategy    = findStrategy(requestPath);

        if (strategy != null) {
            PathVersion path = strategy.getVersion(requestPath);

            if (path != null) {
                String version = path.version();
                String cleared = strategy.removeVersion(requestPath, version);

                newQuery = new ResourceQuery(cleared, query.locations());

                if (next.proceed(context, newQuery) instanceof Outcome.Done<Resource>(Resource resource)) {
                    if (resource == null) {
                        throw new ResourceNotFoundException("Resource '%s' not found".formatted(cleared));
                    }
                    return Outcome.done(new VersionalResource(resource, version));
                }
            }
        }

        return Outcome.next();
    }

    /**
     * 🔍 Find first matching version strategy for a given path.
     *
     * @param requestPath incoming path
     * @return matching strategy or {@code null} if none found
     */
    private VersionStrategy findStrategy(String requestPath) {
        for (VersionStrategy versionStrategy : strategies) {
            if (versionStrategy.isSupports(requestPath)) {
                return versionStrategy;
            }
        }
        return null;
    }
}
