package org.jmouse.jdbc;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeansScannerBeanContextInitializer;
import org.jmouse.beans.DefaultBeanContext;
import org.jmouse.jdbc.bind.MapParameterSource;
import org.jmouse.jdbc.bind.ParameterSource;
import org.jmouse.jdbc.core.NamedOperations;
import org.jmouse.jdbc.core.SimpleOperations;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public class DemoA {

    public static void main(String... arguments) throws SQLException {
        BeanContext context = new DefaultBeanContext(JdbcSupport.class);
        context.setContextId("jdbcContext");
        context.refresh();
        context.addInitializer(new BeansScannerBeanContextInitializer());
        context.refresh();

        System.out.println(context);

        NamedOperations template = context.getBean(NamedOperations.class);


        ParameterSource ps = new MapParameterSource(Map.of("name", "  john  "));

        List<User> users = template.query(
                "select id, name from users where upper(name) = :name|upper|trim",
                ps,
                rs -> new User(rs.getLong("id"), rs.getString("name"))
        );
    }

    record User(long id, String name) {}

}
