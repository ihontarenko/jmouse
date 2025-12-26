package org.jmouse.jdbc;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeansScannerBeanContextInitializer;
import org.jmouse.beans.DefaultBeanContext;
import org.jmouse.core.bind.descriptor.Describer;
import org.jmouse.core.bind.descriptor.structured.ObjectDescriptor;
import org.jmouse.core.bind.descriptor.structured.PropertyDescriptor;
import org.jmouse.jdbc.core.CoreOperations;
import org.jmouse.jdbc.mapping.BeanRowMapper;
import org.jmouse.jdbc.mapping.ColumnRowMapper;

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

        operations.queryOne("select upper('John') as name, 123456 as id",
                         new ColumnRowMapper<>(2, ResultSet::getObject));

        ObjectDescriptor<User> descriptor = Describer.forObjectDescriptor(User.class);
        PropertyDescriptor<User> propertyDescriptor = descriptor.getProperty("name");

        System.out.println(client);
    }

    public record User(int id, String name) {}


}
