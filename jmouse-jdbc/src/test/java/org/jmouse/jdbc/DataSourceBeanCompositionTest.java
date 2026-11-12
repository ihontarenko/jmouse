package org.jmouse.jdbc;

import static org.junit.jupiter.api.Assertions.*;

import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.jmouse.beans.BeansScannerBeanContextInitializer;
import org.jmouse.context.AbstractApplicationBeanContext;
import org.jmouse.jdbc.connection.datasource.DataSourceContributor;
import org.jmouse.jdbc.connection.datasource.DataSourceKeyHolder;
import org.jmouse.jdbc.connection.datasource.DataSourceSpecification;
import org.jmouse.transaction.infrastructure.support.TransactionContextAccessSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class DataSourceBeanCompositionTest {

    @AfterEach
    void clear() {
        DataSourceKeyHolder.clear();
        TransactionContextAccessSupport.current().clear();
    }

    @Test
    void suppliedDataSourceBypassesLegacyRoutingInfrastructure() throws Exception {
        JdbcDataSource source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:directbean");
        AbstractApplicationBeanContext context = context();
        context.registerBean(DataSource.class, source);
        context.refresh();
        assertSame(source, context.getBean(DataSource.class));
        assertEquals(
            42,
            context.getBean(JdbcTemplate.class).<Integer>queryOne("select 42", (row, index) -> row.getInt(1))
        );
        assertNull(DataSourceKeyHolder.current());
    }

    @Test
    void existingContributorConfigurationStillWorks() throws Exception {
        AbstractApplicationBeanContext context = context();
        context.registerBean(
            DataSourceContributor.class,
            (DataSourceContributor) registry ->
                registry.register(
                    new DataSourceSpecification(
                        "legacy",
                        null,
                        "jdbc:h2:mem:legacybean",
                        "sa",
                        "",
                        null,
                        null,
                        null,
                        null
                    )
                )
        );
        context.refresh();
        try (var scope = DataSourceKeyHolder.withKey("legacy")) {
            assertEquals(
                7,
                context
                    .getBean(JdbcTemplate.class)
                    .<Integer>queryOne("select 7", (row, index) -> row.getInt(1))
            );
        }
    }

    private AbstractApplicationBeanContext context() {
        AbstractApplicationBeanContext context = new AbstractApplicationBeanContext();
        context.refresh();
        context.registerBean(
            org.jmouse.core.io.ResourceLoader.class,
            new org.jmouse.core.io.CompositeResourceLoader()
        );
        context.registerBean(
            org.jmouse.core.environment.Environment.class,
            new org.jmouse.core.environment.StandardEnvironment()
        );
        context.registerBean(
            org.jmouse.core.convert.Conversion.class,
            new org.jmouse.context.conversion.ContextConversion()
        );
        context.addBaseClasses(JdbcSupport.class);
        context.addInitializer(new BeansScannerBeanContextInitializer());
        return context;
    }
}
