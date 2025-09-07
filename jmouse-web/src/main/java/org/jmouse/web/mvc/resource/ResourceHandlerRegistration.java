package org.jmouse.web.mvc.resource;

import org.jmouse.core.matcher.ant.AntMatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ResourceHandlerRegistration {

    private final List<String>              locations    = new ArrayList();
    private final List<ResourceTransformer> transformers = new ArrayList();
    private final List<ResourceResolver>    resolvers    = new ArrayList();
    private       AntMatcher                antMatcher;

    public ResourceHandlerRegistration addResourceLocations(String... locations) {
        this.locations.addAll(Arrays.asList(locations));
        return this;
    }

    public ResourceHandlerRegistration addResourceResolver(ResourceResolver resolvers) {
        this.resolvers.add(resolvers);
        return this;
    }

    public ResourceHandlerRegistration addResourceResolvers(ResourceResolver... resolvers) {
        List.of(resolvers).forEach(this::addResourceResolver);
        return this;
    }

    public ResourceHandlerRegistration addResourceTransformer(ResourceTransformer transformer) {
        this.transformers.add(transformer);
        return this;
    }

    public ResourceHandlerRegistration addResourceTransformers(ResourceTransformer... transformers) {
        List.of(transformers).forEach(this::addResourceTransformer);
        return this;
    }

    public AntMatcher getAntMatcher() {
        return antMatcher;
    }

    public void setAntMatcher(AntMatcher antMatcher) {
        this.antMatcher = antMatcher;
    }
}
