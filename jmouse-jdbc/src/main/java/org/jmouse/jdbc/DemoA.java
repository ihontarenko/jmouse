package org.jmouse.jdbc;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeansScannerBeanContextInitializer;
import org.jmouse.beans.DefaultBeanContext;
import org.jmouse.jdbc.core.NamedOperations;
import org.jmouse.jdbc.core.SimpleOperations;
import org.jmouse.jdbc.mapping.BeanRowMapper;
import org.jmouse.jdbc.mapping.RowMapper;

import java.sql.SQLException;
import java.util.List;

public class DemoA {

    public static void main(String... arguments) throws SQLException {
        BeanContext context = new DefaultBeanContext(JdbcSupport.class);
        context.setContextId("jdbcContext");
        context.refresh();
        context.addInitializer(new BeansScannerBeanContextInitializer());
        context.refresh();

        SimpleOperations simple = context.getBean(SimpleOperations.class);
        NamedOperations named = context.getBean(NamedOperations.class);

        RowMapper<User> userMapper = new BeanRowMapper<>(User.class);

        List<User> users = named.query(
                "select id, name from users",
                userMapper
        );

        System.out.println(users);
    }

    record User(long id, String name) {}

}
