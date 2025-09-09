package org.jmouse.web.mvc.resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class ResourceChainRegistration {

    private final List<ResourceTransformer> transformers = new ArrayList();
    private final List<ResourceResolver>    resolvers    = new ArrayList();

    public ResourceChainRegistration addResolvers(ResourceResolver... resolvers) {
        this.resolvers.addAll(Arrays.asList(resolvers));
        return this;
    }

    public ResourceChainRegistration addTransformers(ResourceTransformer... transformers) {
        this.transformers.addAll(Arrays.asList(transformers));
        return this;
    }

    public List<ResourceTransformer> getTransformers() {
        return Collections.unmodifiableList(transformers);
    }

    public List<ResourceResolver> getResolvers() {
        return Collections.unmodifiableList(resolvers);
    }
}
