package org.jmouse.jdbc;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeansScannerBeanContextInitializer;
import org.jmouse.beans.DefaultBeanContext;
import org.jmouse.core.bind.Bean;
import org.jmouse.core.bind.ValueObject;
import org.jmouse.core.bind.accessor.JavaBeanAccessor;
import org.jmouse.jdbc.core.CoreOperations;

import java.sql.ResultSet;
import java.sql.SQLException;

public class Test {

    public static void main(String[] args) throws SQLException {
        BeanContext context = new DefaultBeanContext(JdbcClient.class);
        context.setContextId("jdbcContext");
        context.refresh();
        context.addInitializer(new BeansScannerBeanContextInitializer());
        context.refresh();

        System.out.println(context);

        JdbcClient client = context.getBean(JdbcClient.class);

        CoreOperations operations = client.jdbc().core();

        ResultSet resultSet = null;

        Bean<User> bean = ValueObject.of(User.class);

        System.out.println(client);
    }

    public record User(int id, String name) {}


}
