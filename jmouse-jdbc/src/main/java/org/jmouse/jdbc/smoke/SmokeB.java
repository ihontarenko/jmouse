package org.jmouse.jdbc.smoke;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeansScannerBeanContextInitializer;
import org.jmouse.beans.DefaultBeanContext;
import org.jmouse.beans.EventBridgeContextInitializer;
import org.jmouse.beans.events.BeanEventDeduplicateKeyStrategy;
import org.jmouse.core.events.DeduplicatingPublishPolicy;
import org.jmouse.core.events.EventPublishPolicy;
import org.jmouse.jdbc.JdbcOperations;
import org.jmouse.jdbc.JdbcSupport;
import org.jmouse.jdbc.NamedOperations;
import org.jmouse.jdbc.connection.datasource.DataSourceContributor;
import org.jmouse.jdbc.connection.datasource.DataSourceKeyHolder;
import org.jmouse.jdbc.connection.datasource.DataSourceSpecification;
import org.jmouse.jdbc.operation.ListQuery;
import org.jmouse.jdbc.operation.OptionalQuery;
import org.jmouse.jdbc.operation.SingleQuery;
import org.jmouse.jdbc.operation.SqlSourceOperation;
import org.jmouse.jdbc.operation.SqlUpdate;
import org.jmouse.jdbc.operation.annotation.InlineSql;
import org.jmouse.jdbc.operation.annotation.SqlResource;
import org.jmouse.jdbc.operation.source.InlineSqlSource;
import org.jmouse.jdbc.operation.source.ResourceSqlSource;
import org.jmouse.jdbc.operation.source.SqlSource;
import org.jmouse.jdbc.operation.template.SqlOperationTemplate;
import org.jmouse.jdbc.statement.QueryStatementHandlerProvider;
import org.jmouse.jdbc.statement.StatementConfigurer;
import org.jmouse.jdbc.statement.StatementConfigurerProvider;
import org.jmouse.jdbc.statement.StatementHandler;
import org.jmouse.jdbc.statement.handler.TimingSinkStatementHandler;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class SmokeB {

    public static void main(String... arguments) throws Exception {
        BeanContext context = new DefaultBeanContext(JdbcSupport.class);
        context.setContextId("JDBC-CONTEXT");
        context.addInitializer(new BeansScannerBeanContextInitializer());
        context.addInitializer(new EventBridgeContextInitializer());

        context.onBeanLookupStarted(payload -> {
            System.out.println("EVENT:Lookup: Bean: " + payload.requiredType() + " | " + payload.beanName());
        });

        DataSourceKeyHolder.use("mysql");

        ((DefaultBeanContext) context).setPublishPolicy(
                new DeduplicatingPublishPolicy(
                        EventPublishPolicy.publishAll(),
                        new BeanEventDeduplicateKeyStrategy()
                )
        );

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

        JdbcOperations jdbcOperations = context.getBean(JdbcOperations.class);
        NamedOperations namedOperations = context.getBean(NamedOperations.class);
        SqlOperationTemplate operationTemplate = context.getBean(SqlOperationTemplate.class);

        System.out.println("JdbcOperations      : " + jdbcOperations.getClass().getName());
        System.out.println("NamedOperations     : " + namedOperations.getClass().getName());
        System.out.println("SqlOperationTemplate: " + operationTemplate.getClass().getName());

        /*
         * 1) Inline source operation via SqlSourceOperation
         */

        TimingSinkStatementHandler.Sink sink = new TimingSinkStatementHandler.InMemoryTimingSink();
        TimingSinkStatementHandler<ResultSet> statementHandler = new TimingSinkStatementHandler<>(sink);

        List<Long> inlineSourceIds = operationTemplate.query(new SelectUserIdsInlineSource(statementHandler));
        System.out.println("RESULT:inline-source:list = " + inlineSourceIds);

        /*
         * 2) Resource source operation via SqlSourceOperation
         * Requires file: classpath: sql/users/select-user-ids.sql
         */
        List<Long> resourceSourceIds = operationTemplate.query(new SelectUserIdsResourceSource());
        System.out.println("RESULT:resource-source:list = " + resourceSourceIds);

        /*
         * 3) Inline SQL declared through annotation
         */
        List<Long> annotationInlineIds = operationTemplate.query(new SelectUserIdsAnnotationInline());
        System.out.println("RESULT:annotation-inline:list = " + annotationInlineIds);

        /*
         * 4) Resource SQL declared through annotation
         * Requires file: classpath: sql/users/select-active-user-ids.sql
         */
//        List<Long> annotationResourceIds = operationTemplate.query(new SelectActiveUserIdsAnnotationResource());
//        System.out.println("RESULT:annotation-resource:list = " + annotationResourceIds);
//
//        /*
//         * 5) Parameterized optional query via record + BeanParameterSource
//         */
//        Optional<Long> optionalId = operationTemplate.queryOptional(new SelectUserIdByUsername("admin"));
//        System.out.println("RESULT:optional = " + optionalId);
//
//        /*
//         * 6) Parameterized single query via record + BeanParameterSource
//         */
//        Long requiredId = operationTemplate.queryOne(new SelectRequiredUserIdByUsername("admin"));
//        System.out.println("RESULT:single = " + requiredId);
//
//        /*
//         * 7) Update operation via annotation
//         */
//        int affected = operationTemplate.update(new DeleteInactiveUsers(30));
//        System.out.println("RESULT:update = " + affected);
//
//        /*
//         * 8) Parameterless class-based execution
//         */
//        List<Long> classBasedIds = operationTemplate.query(SelectUserIdsByClassExecution.class);
//        System.out.println("RESULT:class-based = " + classBasedIds);
//
//        /*
//         * 9) The same parameterless operation executed as an instance
//         */
//        List<Long> classInstanceIds = operationTemplate.query(new SelectUserIdsByClassExecution());
//        System.out.println("RESULT:class-instance = " + classInstanceIds);

        System.out.println(context);
    }

    /**
     * Approach 1:
     * SQL source provided directly by operation through InlineSqlSource.
     */
    public record SelectUserIdsInlineSource(
            TimingSinkStatementHandler<ResultSet> handler
    ) implements ListQuery<Long>, SqlSourceOperation, QueryStatementHandlerProvider, StatementConfigurerProvider {

        @Override
        public Class<Long> elementType() {
            return Long.class;
        }

        @Override
        public SqlSource sqlSource() {
            return new InlineSqlSource("select id from users where name like :name or :name is null and active = :status");
        }

        @Override
        public StatementHandler<ResultSet> queryStatementHandler() {
            return handler;
        }

        @Override
        public StatementConfigurer statementConfigurer() {
            return statement -> {
                statement.setFetchSize(100);
                statement.setMaxRows(1000);
            };
        }

    }

    /**
     * Approach 2:
     * SQL source provided directly by operation through ResourceSqlSource.
     */
    public record SelectUserIdsResourceSource() implements ListQuery<Long>, SqlSourceOperation {

        @Override
        public Class<Long> elementType() {
            return Long.class;
        }

        @Override
        public SqlSource sqlSource() {
            return new ResourceSqlSource("sql/users/select-user-ids.sql");
        }

    }

    /**
     * Approach 3:
     * Inline SQL declared through annotation.
     */
    @InlineSql("select id from users")
    public record SelectUserIdsAnnotationInline() implements ListQuery<Long> {

        @Override
        public Class<Long> elementType() {
            return Long.class;
        }

    }

    /**
     * Approach 4:
     * Resource SQL declared through annotation.
     */
    @SqlResource("sql/users/select-active-user-ids.sql")
    public record SelectActiveUserIdsAnnotationResource() implements ListQuery<Long> {

        @Override
        public Class<Long> elementType() {
            return Long.class;
        }

    }

    /**
     * Approach 5:
     * Parameterized query through record state.
     * BeanParameterSource resolves ":username".
     */
    @InlineSql("select id from users where username = :username")
    public record SelectUserIdByUsername(String username) implements OptionalQuery<Long> {

        @Override
        public Class<Long> elementType() {
            return Long.class;
        }

    }

    /**
     * Approach 6:
     * Required single-result query.
     */
    @InlineSql("select id from users where username = :username")
    public record SelectRequiredUserIdByUsername(String username) implements SingleQuery<Long> {

        @Override
        public Class<Long> elementType() {
            return Long.class;
        }

    }

    /**
     * Approach 7:
     * Update operation with record parameters.
     */
    @InlineSql("delete from users where active = false and last_login_days > :days")
    public record DeleteInactiveUsers(Integer days) implements SqlUpdate {
    }

    /**
     * Approach 8:
     * Parameterless class-based execution.
     */
    @InlineSql("select id from users order by id")
    public static final class SelectUserIdsByClassExecution implements ListQuery<Long> {

        @Override
        public Class<Long> elementType() {
            return Long.class;
        }

    }

}