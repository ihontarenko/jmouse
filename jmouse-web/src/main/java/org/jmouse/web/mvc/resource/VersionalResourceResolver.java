package org.jmouse.web.mvc.resource;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Outcome;
import org.jmouse.core.io.Resource;
import org.jmouse.core.matcher.ant.AntMatcher;
import org.jmouse.web.http.request.RequestPath;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 🧩 Resolves versioned static resource paths using pluggable strategies.
 * Examples:
 * <ul>
 *  <li> Content-hash in filename:   {@code /app-<hash>.js}  → strip to {@code /app.js}, validate by hashing content </li>
 *  <li> Fixed prefix segment:       {@code /v123/app.js}    → strip {@code /v123/}, accept only that fixed version </li>
 *  <li> Query parameter (?v=...):   {@code /app.js?v=123}   → keep path, validate by policy </li>
 * </ul>
 * <p>Chain contract:
 *  <li> If a strategy extracts a version and the underlying resource is found+valid → Done(resource) </li>
 *  <li> If strategy doesn't apply or validation fails → Outcome.next() (let others try)</li> </p>
 */
public class VersionalResourceResolver extends AbstractResourceResolver {

    /**
     * 📋 Registered version strategies.
     */
    private final Map<AntMatcher, VersionStrategy> strategies = new LinkedHashMap<>();

    public VersionalResourceResolver() {
        super(null);
        setComposer(new Composer());
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
            HttpServletRequest context, ResourceQuery query, Chain<HttpServletRequest, ResourceQuery, Resource> next) {
        String          requestPath = query.path();
        VersionStrategy strategy    = findStrategy(RequestPath.ofRequest(context).path());

        if (strategy != null) {
            PathVersion path = strategy.getVersion(requestPath);
            if (path != null) {
                String        version  = path.version();
                String        pure     = strategy.removeVersion(requestPath, version);
                ResourceQuery newQuery = new ResourceQuery(pure, query.locations());

                if (next.proceed(context, newQuery) instanceof Outcome.Done<Resource>(Resource resource)) {
                    if (resource == null) {
                        throw new ResourceNotFoundException(
                                "Resource '%s' not found".formatted(pure));
                    }

                    if (!strategy.validateVersion(resource, version)) {
                        throw new ResourceValidationFailedException(
                                "Resource '%s' version '%s' is not valid".formatted(requestPath, version));
                    }

                    return Outcome.done(new VersionalResource(resource, version));
                }
            }
        }

        return Outcome.next();
    }

    /**
     * ➕ Register a version strategy for a specific matcher.
     *
     * @param matcher  ant-style matcher
     * @param strategy strategy to apply for matching paths
     */
    public VersionalResourceResolver addStrategy(AntMatcher matcher, VersionStrategy strategy) {
        strategies.put(matcher, strategy);
        return this;
    }

    /**
     * ➕ Register a version strategy for one or more ant-style patterns.
     *
     * @param strategy strategy to apply
     * @param patterns ant-style patterns (e.g. {@code /v1/**}, {@code /static/**})
     */
    public VersionalResourceResolver addStrategy(VersionStrategy strategy, String... patterns) {
        for (String pattern : patterns) {
            addStrategy(AntMatcher.of(pattern), strategy);
        }
        return this;
    }

    /**
     * 🔍 Find the first matching version strategy for a given path.
     *
     * @param requestPath incoming path
     * @return matching strategy or {@code null} if none found
     */
    private VersionStrategy findStrategy(String requestPath) {
        VersionStrategy strategy = null;

        for (var entry : strategies.entrySet()) {
            if (entry.getKey().matches(requestPath)) {
                strategy = entry.getValue();
                break;
            }
        }

        return strategy;
    }

    /**
     * 🧱 URL composer: injects a version segment/token into outgoing resource URLs.
     *
     * <p>Flow:</p>
     * <ul>
     *   <li>Find a matching {@link VersionStrategy} for the path.</li>
     *   <li>If the path is already versioned for that strategy &mdash; return it unchanged.</li>
     *   <li>Otherwise attempt {@link VersionStrategy#putVersion(PathVersion)}:
     *     <ul>
     *       <li>For {@link ContentHashVersionStrategy} compute the hash from the resolved {@link Resource} (if available).</li>
     *       <li>For fixed strategies the strategy supplies its own version segment.</li>
     *     </ul>
     *   </li>
     *   <li>If the strategy cannot generate a version (e.g. hashing needs a resource that is not resolvable) &mdash; delegate to {@code next}.</li>
     * </ul>
     */
    public class Composer implements ResourceComposer {

        /**
         * Compose a versioned URL for the given relative path or delegate further down the chain.
         *
         * @param relativePath path relative to the static mount (may include a query string)
         * @param context      composer context (provides access to the current resolver and optional resource resolution)
         * @param next         next composer in the chain
         * @return {@link Outcome#done(Object)} with a versioned URL string, or {@link Outcome#next()} to continue the chain
         */
        @Override
        public Outcome<String> handle(
                String relativePath, UrlComposerContext context, Chain<String, UrlComposerContext, String> next) {
            VersionalResourceResolver resolver = VersionalResourceResolver.this;
            VersionStrategy           strategy = resolver.findStrategy(context.requestPath());

            if (strategy == null) {
                return Outcome.next();
            }

            // Already versioned for this strategy? Keep as-is.
            PathVersion existing = strategy.getVersion(relativePath);

            if (existing != null) {
                return Outcome.done(relativePath);
            }

            String version = strategy.generateVersion(context.resource());
            String with    = strategy.putVersion(new PathVersion(relativePath, version));

            return Outcome.done(with);
        }
    }


}
