package svit.env;

public class EnvironmentProperties implements PropertySupplier {

    @Override
    public void setProperty(String name, Object value) {
        System.setProperty(name, value.toString());
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
        return (T) System.getProperty(name, String.valueOf(defaultValue));
    }

}
