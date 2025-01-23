package svit.env;

import java.util.Properties;

public class JavaProperties implements PropertySupplier {

    private final Properties properties;

    public JavaProperties(Properties properties) {
        this.properties = properties;
    }

    @Override
    public void setProperty(String name, Object value) {
        this.properties.setProperty(name, value.toString());
    }

    @Override
    public <T> T getProperty(String name) {
        return getProperty(name, null);
    }

    @Override
    public <T> T getProperty(String name, T defaultValue) {
        return getProperty(name, null, defaultValue);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public <T> T getProperty(String name, Class<T> type, T defaultValue) {
        return this.properties.containsKey(name) ? (T) this.properties.getProperty(name) : defaultValue;
    }

}
