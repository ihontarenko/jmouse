package org.jmouse.jdbc;

import org.jmouse.beans.BeanContext;
import org.jmouse.beans.BeansScannerBeanContextInitializer;
import org.jmouse.beans.DefaultBeanContext;
import org.jmouse.el.ExpressionLanguage;
import org.jmouse.jdbc.bind.*;
import org.jmouse.jdbc.core.CoreOperations;
import org.jmouse.jdbc.mapping.ColumnRowMapper;
import org.jmouse.jdbc.parameters.SQLCompiled;
import org.jmouse.jdbc.parameters.SQLParameterProcessor;
import org.jmouse.jdbc.parameters.SQLParsed;
import org.jmouse.jdbc.parameters.bind.SQLPlanPreparedStatementBinder;
import org.jmouse.jdbc.parameters.compile.SQLPlanCompiler;
import org.jmouse.jdbc.parameters.lexer.SQLParameterSplitter;
import org.jmouse.jdbc.parameters.lexer.SQLParameterTokenizer;
import org.jmouse.jdbc.statement.PreparedStatementBinder;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class DemoA {

    public static void main(String... arguments) throws SQLException {
        BeanContext context = new DefaultBeanContext(JdbcClient.class);
        context.setContextId("jdbcContext");
        context.refresh();
        context.addInitializer(new BeansScannerBeanContextInitializer());
        context.refresh();

        System.out.println(context);

        JdbcClient client = context.getBean(JdbcClient.class);

        CoreOperations operations = client.jdbc().core();

        operations.queryOne("select * from t where a = :x|trim and b = ? and c = :x", binder(),
                new ColumnRowMapper<>(2, ResultSet::getObject));
    }

    public static PreparedStatementBinder binder() {
        SQLParameterSplitter  splitter  = new SQLParameterSplitter();
        SQLParameterTokenizer tokenizer = new SQLParameterTokenizer(splitter);

        ExpressionLanguage el       = new ExpressionLanguage();
        SQLPlanCompiler    compiler = new SQLPlanCompiler(el);

        SQLParameterProcessor processor = new SQLParameterProcessor(tokenizer, compiler);

        String      sql      = "select * from t where a = :x|trim and b = ? and c = :x";
        SQLParsed   parsed   = processor.parse("UserByName", sql);
        SQLCompiled compiled = processor.compile(parsed);

        ParameterSource n = new MapParameterSource(Map.of("x", "  AAA "));
        ParameterSource p = new ArrayParameterSource(new Object[]{77});
        ParameterSource c = CompositeParameterSource.of(n, p);

        return new SQLPlanPreparedStatementBinder(el, compiled.plan(), c, MissingParameterPolicy.FAIL_FAST);
    }

}
