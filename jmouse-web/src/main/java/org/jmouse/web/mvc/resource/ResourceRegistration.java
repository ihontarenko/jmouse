package org.jmouse.web.mvc.resource;

import org.jmouse.core.matcher.ant.AntMatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ResourceRegistration {

    private final List<String>              locations    = new ArrayList();
    private final List<AntMatcher>          matchers    = new ArrayList();
    private final List<ResourceTransformer> transformers = new ArrayList();
    private final List<ResourceResolver>    resolvers    = new ArrayList();

    public ResourceRegistration addAntMatchers(AntMatcher... antMatchers) {
        this.matchers.addAll(Arrays.asList(antMatchers));
        return this;
    }

    public ResourceRegistration addResourceLocations(String... locations) {
        this.locations.addAll(Arrays.asList(locations));
        return this;
    }

    public ResourceRegistration addResourceResolvers(ResourceResolver... resolvers) {
        this.resolvers.addAll(Arrays.asList(resolvers));
        return this;
    }

    public ResourceRegistration addResourceTransformers(ResourceTransformer... transformers) {
        this.transformers.addAll(Arrays.asList(transformers));
        return this;
    }

    public List<String> getLocations() {
        return locations;
    }

    public List<AntMatcher> getMatchers() {
        return matchers;
    }

    public List<ResourceTransformer> getTransformers() {
        return transformers;
    }

    public List<ResourceResolver> getResolvers() {
        return resolvers;
    }

    @Override
    public String toString() {
        return "RESOURCE_REGISTRATION: [%s] %s".formatted(matchers, locations);
    }
}
