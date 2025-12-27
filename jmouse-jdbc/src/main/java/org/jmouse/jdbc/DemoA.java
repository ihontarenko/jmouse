package org.jmouse.jdbc;

import org.jmouse.el.ExpressionLanguage;
import org.jmouse.jdbc.parameters.SQLCompiled;
import org.jmouse.jdbc.parameters.SQLParameterProcessor;
import org.jmouse.jdbc.parameters.SQLParsed;
import org.jmouse.jdbc.parameters.compile.SQLPlanCompiler;
import org.jmouse.jdbc.parameters.lexer.SQLParameterSplitter;
import org.jmouse.jdbc.parameters.lexer.SQLParameterTokenizer;

public class DemoA {

    public static void main(String... arguments) {
        SQLParameterSplitter  splitter  = new SQLParameterSplitter();
        SQLParameterTokenizer tokenizer = new SQLParameterTokenizer(splitter);

        ExpressionLanguage el       = new ExpressionLanguage();
        SQLPlanCompiler    compiler = new SQLPlanCompiler(el);

        SQLParameterProcessor processor = new SQLParameterProcessor(tokenizer, compiler);

        String      sql      = "select * from users where id = ? and name = :name|upper|trim";
        SQLParsed   parsed   = processor.parse("UserByName", sql);
        SQLCompiled compiled = processor.compile(parsed);

        System.out.println(compiled.compiled());
    }

}
