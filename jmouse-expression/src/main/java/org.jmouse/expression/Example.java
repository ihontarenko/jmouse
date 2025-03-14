package org.jmouse.expression;

import org.jmouse.common.ast.compiler.EvaluationContextFactory;
import org.jmouse.common.ast.lexer.Lexer;
import org.jmouse.common.ast.compiler.EvaluationContext;
import org.jmouse.common.ast.node.Node;
import org.jmouse.common.ast.node.RootNode;
import org.jmouse.common.ast.parser.Parser;
import org.jmouse.common.ast.parser.ParserContext;
import org.jmouse.common.ast.token.Token;
import org.jmouse.common.ast.token.Tokenizer;
import org.jmouse.expression.compiler.EvaluationContextConfigurator;
import org.jmouse.expression.parser.ParserConfigurator;
import org.jmouse.expression.parser.AnyExpressionParser;
import org.jmouse.util.helper.Booleans;
import org.jmouse.util.helper.Maths;
import org.jmouse.expression.test.ExampleDto;
import org.jmouse.expression.test.TestService;

import java.util.List;

public class Example {

    public static void main(String[] args) {
        ParserContext   parserContext = ParserContext.CONTEXT;
        Tokenizer       tokenizer     = new DefaultTokenizer();
        EvaluationContext evaluationContext = EvaluationContextFactory.defaultEvaluationContext(
                TestService.class, Maths.class, Booleans.class);

        evaluationContext.setVariable("user", "Ivan");
        evaluationContext.setVariable("item", new ExampleDto());

        new TokenizerConfigurator().configure(tokenizer);
        new ParserConfigurator().configure(parserContext);
        new EvaluationContextConfigurator().configure(evaluationContext);

        String el1 = "(values={#validation/non_null, (applier=#lte(#item.getQty(), 100), qty=#item.getQty(), name=#item.getName()), #user.startsWith('Iv'), #lt(1, 2), #multiply(7, #divide(22, 7)), #divide(22, 7), svit.tag.Example, 1, #user, (key1=123, key2='Hello!', key3={1, 2.3, 3.123123123123, #math.sum(456, 9881), java.util.List}, key4=#user)})";
        String el2 = "#validation/non_null";
        String el3 = "/user/:id/:name/action:do";

        List<Token.Entry> entries = tokenizer.tokenize(el3);

        entries = entries.stream().filter(e -> e.token() == ExtendedToken.T_PATH_VARIABLE).toList();

        Lexer lexer = new DefaultLexer(entries);

        Node   root   = new RootNode();
        Parser parser = parserContext.getParser(AnyExpressionParser.class);

        parser.parse(lexer, root, parserContext);

        evaluationContext.setVariable("service", new TestService());
        evaluationContext.setVariable("math", new TestService());

        Object params = root.first().first().evaluate(evaluationContext);

        System.out.println(params);
    }

}
