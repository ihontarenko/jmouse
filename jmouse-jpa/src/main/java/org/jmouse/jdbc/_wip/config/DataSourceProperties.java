package org.jmouse.jdbc._wip.config;

import org.jmouse.util.StringHelper;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 🧾 Unified JDBC properties container.
 */
public final class DataSourceProperties {

    public static final String POOL_PREFIX       = "pool.";
    public static final String JDBC_POOL_PREFIX  = "jdbc.pool.";
    public static final String JDBC_URL          = "jdbc.url";
    public static final String URL               = "url";
    public static final String JDBC_USERNAME     = "jdbc.username";
    public static final String JDBC_USER         = "jdbc.user";
    public static final String USERNAME          = "username";
    public static final String USER              = "user";
    public static final String PASSWORD          = "password";
    public static final String JDBC_PASSWORD     = "jdbc.password";
    public static final String JDBC_DRIVER       = "jdbc.driver";
    public static final String JDBC_JNDI_NAME    = "jdbc.jndiName";
    public static final String POOL_NAME         = POOL_PREFIX + ".name";
    public static final String JDBC_POOL_NAME    = JDBC_POOL_PREFIX + ".name";
    public static final String JNDI_NAME         = "jndiName";
    public static final String DRIVER_CLASS_NAME = "driverClassName";

    public String username;
    public String password;
    public String url;
    public String driverClassName; // optional; can be auto-resolved
    public String jndiName;        // optional; if set, JNDI lookup wins
    public String poolName;        // optional; e.g. "hikari", "tomcat", "dbcp2"

    /**
     * Pool-specific flat properties (maxSize, minIdle, etc.).
     */
    public final Map<String, Object> pool = new LinkedHashMap<>();

    public DataSourceProperties with(String key, Object value) {
        pool.put(key, value);
        return this;
    }

    /**
     * Convenience factory from a flat Map (spring-like keys supported).
     */
    public static DataSourceProperties ofMap(Map<String, ?> properties) {
        DataSourceProperties sourceProperties = new DataSourceProperties();

        sourceProperties.url             = StringHelper.getString(properties, JDBC_URL, URL);
        sourceProperties.username        = StringHelper.getString(properties, JDBC_USERNAME, JDBC_USER, USERNAME, USER);
        sourceProperties.password        = StringHelper.getString(properties, JDBC_PASSWORD, PASSWORD);
        sourceProperties.driverClassName = StringHelper.getString(properties, JDBC_DRIVER, DRIVER_CLASS_NAME);
        sourceProperties.jndiName        = StringHelper.getString(properties, JDBC_JNDI_NAME, JNDI_NAME);
        sourceProperties.poolName        = StringHelper.getString(properties, POOL_NAME, JDBC_POOL_NAME);

        properties.forEach((key,value) -> {
            if (key.startsWith(POOL_PREFIX) || key.startsWith(JDBC_POOL_PREFIX)) {
                String poolKey = key.startsWith(POOL_PREFIX) ? key.substring(POOL_PREFIX.length()) : key.substring(JDBC_POOL_PREFIX.length());
                sourceProperties.pool.put(poolKey, value);
            }
        });

        return sourceProperties;
    }

}
