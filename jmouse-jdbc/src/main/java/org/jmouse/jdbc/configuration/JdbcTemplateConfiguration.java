package org.jmouse.jdbc.configuration;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.beans.annotation.Eager;
import org.jmouse.beans.annotation.PrimaryBean;
import org.jmouse.jdbc.*;
import org.jmouse.jdbc.parameters.MissingParameterPolicy;
import org.jmouse.jdbc.parameters.SQLParameterProcessor;
import org.jmouse.jdbc.parameters.compile.SQLPlanCompiler;
import org.jmouse.jdbc.parameters.lexer.SQLParameterSplitter;
import org.jmouse.jdbc.parameters.lexer.SQLParameterTokenizer;
import org.jmouse.jdbc.parameters.named.NamedSqlPreparedExecutionFactory;

@BeanFactories
public class JdbcTemplateConfiguration {

    @Eager
    @Bean
    @PrimaryBean
    public JdbcOperations simpleTemplate(JdbcExecutor executor) {
        return new JdbcTemplate(executor);
    }

    @Bean
    public MissingParameterPolicy missingParameterPolicy() {
        return MissingParameterPolicy.FAIL_FAST;
    }

    @Bean
    public SQLParameterTokenizer sqlParameterTokenizer() {
        return new SQLParameterTokenizer(new SQLParameterSplitter());
    }

    @Bean
    public SQLPlanCompiler sqlPlanCompiler() {
        return new SQLPlanCompiler();
    }

    @Bean
    public SQLParameterProcessor sqlParameterProcessor(SQLParameterTokenizer tokenizer, SQLPlanCompiler compiler) {
        return new SQLParameterProcessor(tokenizer, compiler);
    }

    @Bean
    public NamedSqlPreparedExecutionFactory namedSqlPreparedExecutionFactory(
            SQLParameterProcessor processor,
            MissingParameterPolicy missingPolicy
    ) {
        return new NamedSqlPreparedExecutionFactory(processor, missingPolicy);
    }

    @Bean
    public NamedOperations namedTemplate(
            JdbcExecutor executor, NamedSqlPreparedExecutionFactory executionFactory
    ) {
        return new NamedTemplate(executor, executionFactory);
    }

}
