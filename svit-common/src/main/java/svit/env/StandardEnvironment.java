package svit.env;

public class StandardEnvironment implements Environment {

    private final PlaceholderResolver placeholderResolver;
    private final PropertyResolver propertyResolver;

    public StandardEnvironment(PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;
        this.placeholderResolver = new SimplePlaceholderResolver(this);
    }

    @Override
    public String[] getDefaultProfiles() {
        return new String[0];
    }

    @Override
    public String[] getActiveProfiles() {
        return new String[0];
    }

    @Override
    public boolean acceptsProfile(String profile) {
        return false;
    }

    @Override
    public String resolvePlaceholders(String text) {
        return placeholderResolver.resolvePlaceholders(text);
    }

    @Override
    public String resolveRequiredPlaceholders(String text) {
        return placeholderResolver.resolveRequiredPlaceholders(text);
    }

    @Override
    public void setRegistry(PropertySourceRegistry registry) {
        propertyResolver.setRegistry(registry);
    }

    @Override
    public PropertySourceRegistry getRegistry() {
        return propertyResolver.getRegistry();
    }

    @Override
    public Object getRawProperty(String name) {
        return propertyResolver.getRawProperty(name);
    }

    @Override
    public <T> T getProperty(String name, Class<T> targetType) {
        return propertyResolver.getProperty(name, targetType);
    }
}
