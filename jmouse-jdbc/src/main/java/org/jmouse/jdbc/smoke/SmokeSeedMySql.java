package org.jmouse.jdbc.smoke;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeansScannerBeanContextInitializer;
import org.jmouse.beans.DefaultBeanContext;
import org.jmouse.beans.EventBridgeContextInitializer;
import org.jmouse.beans.events.BeanEventDeduplicateKeyStrategy;
import org.jmouse.core.events.DeduplicatingPublishPolicy;
import org.jmouse.core.events.EventPublishPolicy;
import org.jmouse.jdbc.JdbcTemplate;
import org.jmouse.jdbc.JdbcSupport;
import org.jmouse.jdbc.connection.datasource.DataSourceContributor;
import org.jmouse.jdbc.connection.datasource.DataSourceKeyHolder;
import org.jmouse.jdbc.connection.datasource.DataSourceSpecification;

import java.util.List;

public final class SmokeSeedMySql {

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

        JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);

        jdbc.update("""
                create table if not exists users (
                    id bigint primary key auto_increment,
                    username varchar(128) not null,
                    active boolean not null,
                    last_login_days int not null
                )
                """);

        jdbc.update("delete from users");

        jdbc.update("insert into users(username, active, last_login_days) values ('admin', true, 1)");
        jdbc.update("insert into users(username, active, last_login_days) values ('john', true, 5)");
        jdbc.update("insert into users(username, active, last_login_days) values ('mike', false, 30)");
        jdbc.update("insert into users(username, active, last_login_days) values ('kate', false, 120)");

        List<String> usernames = jdbc.query(
                "select username from users order by id",
                (rs, index) -> rs.getString(1)
        );

        Long total = jdbc.queryOne(
                "select count(*) from users",
                (rs, index) -> rs.getLong(1)
        );

        System.out.println("TOTAL USERS: " + total);
        System.out.println("USERNAMES  : " + usernames);
        System.out.println(context);
    }

}