package org.jmouse.jdbc.configuration;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.jdbc.JdbcOperations;
import org.jmouse.jdbc.operation.StatementOptionsResolver;
import org.jmouse.jdbc.parameters.named.NamedSqlPreparedExecutionFactory;
import org.jmouse.jdbc.operation.resolve.SqlOperationParametersResolver;
import org.jmouse.jdbc.operation.resolve.SqlOperationResolver;
import org.jmouse.jdbc.operation.resolve.SqlTextLoader;
import org.jmouse.jdbc.operation.resolve.support.ClasspathSqlTextLoader;
import org.jmouse.jdbc.operation.template.ReflectionSqlOperationInstantiator;
import org.jmouse.jdbc.operation.template.SqlOperationInstantiator;
import org.jmouse.jdbc.operation.template.SqlOperationRowMapperResolver;
import org.jmouse.jdbc.operation.template.SqlOperationTemplate;
import org.jmouse.jdbc.operation.template.support.DefaultSqlOperationRowMapperResolver;

@BeanFactories
public class JdbcOperationConfiguration {

    @Bean
    public SqlTextLoader sqlTextLoader() {
        return new ClasspathSqlTextLoader();
    }

    @Bean
    public SqlOperationParametersResolver sqlOperationParametersResolver() {
        return new SqlOperationParametersResolver.Default();
    }

    @Bean
    public SqlOperationResolver sqlOperationResolver(
            SqlOperationParametersResolver parametersResolver,
            SqlTextLoader textLoader
    ) {
        return new SqlOperationResolver.Default(parametersResolver, textLoader);
    }

    @Bean
    public StatementOptionsResolver statementOptionsResolver() {
        return new StatementOptionsResolver.Default();
    }

    @Bean
    public SqlOperationInstantiator sqlOperationInstantiator() {
        return new ReflectionSqlOperationInstantiator();
    }

    @Bean
    public SqlOperationRowMapperResolver sqlOperationRowMapperResolver() {
        return new DefaultSqlOperationRowMapperResolver();
    }

    @Bean
    public SqlOperationTemplate sqlOperationTemplate(
            SqlOperationRowMapperResolver rowMapperResolver,
            JdbcOperations operations,
            SqlOperationResolver resolver,
            SqlOperationInstantiator instantiator,
            NamedSqlPreparedExecutionFactory preparedExecutionFactory,
            StatementOptionsResolver statementOptionsResolver
    ) {
        return new SqlOperationTemplate.Default(
                rowMapperResolver,
                operations,
                resolver,
                instantiator,
                preparedExecutionFactory,
                statementOptionsResolver
        );
    }

}