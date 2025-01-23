package svit.env;

public interface PropertySupplier {

    void setProperty(String name, Object value);

    <T> T getProperty(String name);

    <T> T getProperty(String name, T defaultValue);

    <T> T getProperty(String name, Class<T> type, T defaultValue);

}
