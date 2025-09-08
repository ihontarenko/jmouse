package org.jmouse.web.mvc.resource;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.core.chain.Chain;
import org.jmouse.core.chain.Outcome;
import org.jmouse.core.io.Resource;

import java.util.LinkedList;
import java.util.List;

public class VersionalResourceResolver extends AbstractResourceResolver {

    private final List<VersionStrategy>        strategies = new LinkedList<>();

    public VersionalResourceResolver() {
        this.strategies.add(new FixedVersionStrategy("/v1/**", "v1"));
    }

    @Override
    public Outcome<Resource> handle(
            HttpServletRequest context, ResourceQuery resourceQuery, Chain<HttpServletRequest, ResourceQuery, Resource> next) {
        ResourceQuery   newQuery = resourceQuery;
        String          path     = resourceQuery.path();
        VersionStrategy strategy = findStrategy(path);

        if (strategy != null) {
            VersionPath versionPath = strategy.getVersion(path);

            if (versionPath != null) {
                String clearPath = strategy.removeVersion(path, versionPath.version());
                newQuery = new ResourceQuery(clearPath, resourceQuery.locations());
            }

            if (next.proceed(context, newQuery) instanceof Outcome.Done<Resource>(Resource resource)) {
                return Outcome.done(
                        new VersionalResource(resource)
                );
            }
        }

        return next.proceed(context, newQuery);
    }

    private VersionStrategy findStrategy(String requestPath) {
        VersionStrategy strategy = null;

        for (VersionStrategy versionStrategy : strategies) {
            if (versionStrategy.isSupports(requestPath)) {
                strategy = versionStrategy;
                break;
            }
        }

        return strategy;
    }

}
