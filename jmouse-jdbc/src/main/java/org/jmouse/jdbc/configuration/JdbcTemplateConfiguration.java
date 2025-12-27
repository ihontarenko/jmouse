package org.jmouse.jdbc.configuration;

import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.beans.conditions.BeanCondition;
import org.jmouse.jdbc.bind.MissingParameterPolicy;
import org.jmouse.jdbc.core.*;
import org.jmouse.jdbc.parameters.SQLExpressionLanguage;
import org.jmouse.jdbc.parameters.SQLParameterProcessor;
import org.jmouse.jdbc.parameters.compile.SQLPlanCompiler;
import org.jmouse.jdbc.parameters.lexer.SQLParameterSplitter;
import org.jmouse.jdbc.parameters.lexer.SQLParameterTokenizer;

@BeanFactories
public class JdbcTemplateConfiguration {

    @Bean
    public SQLExpressionLanguage sqlExpressionLanguage() {
        return new SQLExpressionLanguage();
    }

    @Bean
    public SimpleOperations simpleTemplate(JdbcExecutor executor) {
        return new SimpleTemplate(executor);
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
    public SQLPlanCompiler sqlPlanCompiler(SQLExpressionLanguage expressionLanguage) {
        return new SQLPlanCompiler(expressionLanguage);
    }

    @Bean
    public SQLParameterProcessor sqlParameterProcessor(SQLParameterTokenizer tokenizer, SQLPlanCompiler compiler) {
        return new SQLParameterProcessor(tokenizer, compiler);
    }

    @Bean
    public NamedOperations namedTemplate(
            JdbcExecutor executor,
            SQLExpressionLanguage expressionLanguage,
            SQLParameterProcessor sqlParameterProcessor,
            MissingParameterPolicy policy
    ) {
        return new NamedTemplate(executor, expressionLanguage, sqlParameterProcessor, policy);
    }

}
