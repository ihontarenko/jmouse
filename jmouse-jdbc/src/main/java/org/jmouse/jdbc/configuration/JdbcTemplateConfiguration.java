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

/**
 * 📦 JDBC template & named SQL infrastructure configuration.
 *
 * <p>Wires core components for:
 * <ul>
 *     <li>Basic JDBC execution via {@link JdbcTemplate}</li>
 *     <li>Named-parameter SQL processing pipeline</li>
 *     <li>SQL parsing, compilation, and binding</li>
 * </ul>
 */
@BeanFactories
public class JdbcTemplateConfiguration {

    /**
     * 🧩 Primary JDBC template.
     *
     * <p>Thin facade over {@link JdbcExecutor} providing
     * high-level query/update operations.</p>
     */
    @Eager
    @Bean
    @PrimaryBean
    public JdbcTemplate simpleTemplate(JdbcExecutor executor) {
        return new JdbcTemplate.Default(executor);
    }

    /**
     * ⚠️ Missing parameter handling strategy.
     *
     * <p>{@link MissingParameterPolicy#FAIL_FAST} ensures
     * unresolved named parameters fail early.</p>
     */
    @Bean
    public MissingParameterPolicy missingParameterPolicy() {
        return MissingParameterPolicy.FAIL_FAST;
    }

    /**
     * 🔍 SQL tokenizer for named parameters.
     *
     * <p>Splits SQL into tokens using {@link SQLParameterSplitter}
     * and identifies parameter placeholders.</p>
     */
    @Bean
    public SQLParameterTokenizer sqlParameterTokenizer() {
        return new SQLParameterTokenizer(new SQLParameterSplitter());
    }

    /**
     * 🧠 SQL execution plan compiler.
     *
     * <p>Transforms tokenized SQL into a compiled plan
     * suitable for prepared execution.</p>
     */
    @Bean
    public SQLPlanCompiler sqlPlanCompiler() {
        return new SQLPlanCompiler();
    }

    /**
     * 🔗 SQL parameter processor.
     *
     * <p>Coordinates tokenization and compilation to produce
     * executable parameter binding metadata.</p>
     */
    @Bean
    public SQLParameterProcessor sqlParameterProcessor(SQLParameterTokenizer tokenizer, SQLPlanCompiler compiler) {
        return new SQLParameterProcessor(tokenizer, compiler);
    }

    /**
     * ⚙️ Named SQL → Prepared execution factory.
     *
     * <p>Converts SQL with named parameters into positional SQL
     * + {@link org.jmouse.jdbc.statement.StatementBinder}.</p>
     */
    @Bean
    public NamedSqlPreparedExecutionFactory namedSqlPreparedExecutionFactory(
            SQLParameterProcessor processor,
            MissingParameterPolicy missingPolicy
    ) {
        return new NamedSqlPreparedExecutionFactory(processor, missingPolicy);
    }

    /**
     * 📦 Named-parameter template.
     *
     * <p>Facade over {@link JdbcExecutor} supporting named parameters
     * via {@link NamedSqlPreparedExecutionFactory}.</p>
     */
    @Bean
    public NamedTemplate namedTemplate(
            JdbcExecutor executor, NamedSqlPreparedExecutionFactory executionFactory
    ) {
        return new NamedTemplate.Default(executor, executionFactory);
    }

}