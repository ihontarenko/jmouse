package org.jmouse.jdbc.intercept;

public interface JdbcInterceptor {
    Object intercept(JdbcInvocation invocation) throws Throwable;
}