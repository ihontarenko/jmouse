package org.jmouse.core.env;

import org.jmouse.util.Arrays;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StandardEnvironment implements Environment {

    private final PropertyResolver    resolver;
    private final PlaceholderResolver placeholder;
    private final Set<String>         profiles = new HashSet<>();

    public StandardEnvironment() {
        this(new StandardPropertyResolver(new DefaultPropertySourceRegistry()));
    }

    public StandardEnvironment(PropertyResolver resolver) {
        this.resolver = resolver;
        this.placeholder = new SimplePlaceholderResolver(this);
    }

    @Override
    public String[] getDefaultProfiles() {
        Set<String> defaultProfiles = new HashSet<>();

        return defaultProfiles.toArray(String[]::new);
    }

    @Override
    public String[] getActiveProfiles() {
        String[] activeProfiles = profiles.toArray(String[]::new);

        if (Arrays.empty(activeProfiles)) {
            String profiles = getProperty("profiles");
            if (profiles != null) {
                activeProfiles = profiles.split(",");
                this.profiles.addAll(List.of(activeProfiles));
            }
        }

        return activeProfiles;
    }

    @Override
    public boolean acceptsProfile(String profile) {
        return Set.of(getActiveProfiles()).contains(profile);
    }

    @Override
    public String resolvePlaceholders(String text) {
        return placeholder.resolvePlaceholders(text);
    }

    @Override
    public String resolveRequiredPlaceholders(String text) {
        return placeholder.resolveRequiredPlaceholders(text);
    }

    @Override
    public void setRegistry(PropertySourceRegistry registry) {
        resolver.setRegistry(registry);
    }

    @Override
    public PropertySourceRegistry getRegistry() {
        return resolver.getRegistry();
    }

    @Override
    public Object getRawProperty(String name) {
        return resolver.getRawProperty(name);
    }

    @Override
    public <T> T getProperty(String name, Class<T> targetType) {
        return resolver.getProperty(name, targetType);
    }
}
