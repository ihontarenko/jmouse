package org.jmouse.jdbc.smoke;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeansScannerBeanContextInitializer;
import org.jmouse.beans.DefaultBeanContext;
import org.jmouse.beans.EventBridgeContextInitializer;
import org.jmouse.beans.events.BeanEventDeduplicateKeyStrategy;
import org.jmouse.core.events.DeduplicatingPublishPolicy;
import org.jmouse.core.events.EventPublishPolicy;
import org.jmouse.jdbc.JdbcSupport;
import org.jmouse.jdbc.NamedOperations;
import org.jmouse.jdbc.connection.datasource.DataSourceContributor;
import org.jmouse.jdbc.connection.datasource.DataSourceKeyHolder;
import org.jmouse.jdbc.connection.datasource.DataSourceSpecification;
import org.jmouse.jdbc.parameters.BeanParameterSource;

import java.util.List;

public final class SmokeNamedQueryMySql {

    public static void main(String... arguments) throws Exception {
        BeanContext context = new DefaultBeanContext(JdbcSupport.class);
        context.setContextId("JDBC-CONTEXT");
        context.addInitializer(new BeansScannerBeanContextInitializer());
        context.addInitializer(new EventBridgeContextInitializer());

        ((DefaultBeanContext) context).setPublishPolicy(
                new DeduplicatingPublishPolicy(
                        EventPublishPolicy.publishAll(),
                        new BeanEventDeduplicateKeyStrategy()
                )
        );

        DataSourceKeyHolder.use("mysql");

        context.refresh();

        context.registerBean(DataSourceContributor.class, (DataSourceContributor) registry ->
                registry.register(new DataSourceSpecification(
                        "mysql",
                        null,
                        "jdbc:mysql://127.0.0.1:3306/jmouse?useUnicode=true&characterEncoding=utf8&serverTimezone=UTC",
                        "jmouse",
                        "jmouse",
                        "jmouse",
                        "mysql",
                        null,
                        null
                ))
        );

        NamedOperations named = context.getBean(NamedOperations.class);

        Filter filter = new Filter(true, 10);

        List<String> usernames = named.query(
                """
                select username
                from users
                where active = :active
                  and last_login_days <= :days
                order by id
                """,
                new BeanParameterSource(filter),
                (rs, index) -> rs.getString(1)
        );

        System.out.println("MATCHED: " + usernames);
    }

    public record Filter(boolean active, int days) {
    }

}