package org.jmouse.jdbc;

import org.jmouse.context.feature.FeatureImport;
import org.jmouse.jdbc.configuration.*;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 🗄️ Enables JDBC infrastructure for jMouse.
 *
 * <p>
 * Activates a full set of JDBC-related configurations required for database access,
 * including connection management, execution, templating, and transaction support.
 * </p>
 *
 * <p>
 * Imported configurations:
 * </p>
 * <ul>
 *     <li>{@link JdbcConnectionProviderConfiguration} — connection provider setup</li>
 *     <li>{@link JdbcDataSourceConfiguration} — data source configuration</li>
 *     <li>{@link JdbcExecutorConfiguration} — low-level JDBC executor</li>
 *     <li>{@link JdbcPlatformConfiguration} — database/platform-specific support</li>
 *     <li>{@link JdbcTemplateConfiguration} — high-level JDBC template abstraction</li>
 *     <li>{@link JdbcTransactionConfiguration} — transaction management integration</li>
 *     <li>{@link MissingDataSourceConfiguration} — fallback handling when no data source is defined</li>
 * </ul>
 *
 * <p>
 * This annotation is typically placed on a root configuration class to bootstrap
 * JDBC support in the application context.
 * </p>
 *
 * <p>
 * 💡 Works seamlessly with feature selection mechanism via {@link FeatureImport}.
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@FeatureImport({
        JdbcConnectionProviderConfiguration.class,
        JdbcDataSourceConfiguration.class,
        JdbcExecutorConfiguration.class,
        JdbcPlatformConfiguration.class,
        JdbcTemplateConfiguration.class,
        JdbcTransactionConfiguration.class,
        MissingDataSourceConfiguration.class
})
public @interface EnableJdbc { }