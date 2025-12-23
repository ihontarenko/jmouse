package org.jmouse.jdbc;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeansScannerBeanContextInitializer;
import org.jmouse.beans.DefaultBeanContext;

import java.sql.SQLException;

public class Test {

    public static void main(String[] args) throws SQLException {
        BeanContext context = new DefaultBeanContext(JdbcClient.class);
        context.setContextId("jdbcContext");
        context.refresh();
        context.addInitializer(new BeansScannerBeanContextInitializer());
        context.refresh();

        System.out.println(context);

        JdbcClient         client             = context.getBean(JdbcClient.class);

        System.out.println(client);
    }

}
