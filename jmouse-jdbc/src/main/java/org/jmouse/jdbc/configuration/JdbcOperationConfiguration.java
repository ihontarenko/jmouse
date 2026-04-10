package org.jmouse.jdbc.configuration;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.el.ExpressionLanguage;
import org.jmouse.jdbc.JdbcOperations;
import org.jmouse.jdbc.operation.StatementOptionsResolver;
import org.jmouse.jdbc.operation.execution.NamedSqlPreparedExecutionFactory;
import org.jmouse.jdbc.operation.resolve.SqlOperationParametersResolver;
import org.jmouse.jdbc.operation.resolve.SqlOperationResolver;
import org.jmouse.jdbc.operation.resolve.SqlTextLoader;
import org.jmouse.jdbc.operation.resolve.support.ClasspathSqlTextLoader;
import org.jmouse.jdbc.operation.template.ReflectionSqlOperationInstantiator;
import org.jmouse.jdbc.operation.template.SqlOperationInstantiator;
import org.jmouse.jdbc.operation.template.SqlOperationRowMapperResolver;
import org.jmouse.jdbc.operation.template.SqlOperationTemplate;
import org.jmouse.jdbc.operation.template.support.DefaultSqlOperationRowMapperResolver;
import org.jmouse.jdbc.parameters.MissingParameterPolicy;
import org.jmouse.jdbc.parameters.SQLParameterProcessor;

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
    public NamedSqlPreparedExecutionFactory namedSqlPreparedExecutionFactory(
            ExpressionLanguage expressionLanguage,
            SQLParameterProcessor processor,
            MissingParameterPolicy missingPolicy
    ) {
        return new NamedSqlPreparedExecutionFactory(expressionLanguage, processor, missingPolicy);
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