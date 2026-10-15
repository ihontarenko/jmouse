package org.jmouse.query.spring.autoconfigure;

import org.jmouse.query.spring.QueryRepositories;
import org.jmouse.query.spring.builder.QueryRunner;
import org.jmouse.query.sql.QueryEngine;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The engine, its repositories and the runner exist exactly when the application has a data source — and that
 * data source comes from Spring Boot's own auto-configuration, which is the case that declined.
 *
 * <p>⚠️ Both auto-configurations are handed over together, in the order that loses: {@link AutoConfigurations}
 * sorts them the way an application does, so without declared ordering {@code org.jmouse…} is asked about a
 * data source before {@code org.springframework…} has registered one, and every bean conditional on it
 * silently backs off.</p>
 */
class QueryAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withPropertyValues("jmouse.query.dialect=postgresql");

    @Test
    @DisplayName("with Boot's data source, the engine, the repositories and the runner are there")
    void activatesWithDataSource() {
        runner.withConfiguration(AutoConfigurations.of(QueryAutoConfiguration.class, DataSourceAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(QueryEngine.class);
                    assertThat(context).hasSingleBean(QueryRunner.class);
                });
    }

    @Test
    @DisplayName("without a data source, nothing that needs one is created")
    void backsOffWithoutDataSource() {
        runner.withConfiguration(AutoConfigurations.of(QueryAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(QueryEngine.class);
                    assertThat(context).doesNotHaveBean(QueryRepositories.class);
                    assertThat(context).doesNotHaveBean(QueryRunner.class);
                });
    }
}
