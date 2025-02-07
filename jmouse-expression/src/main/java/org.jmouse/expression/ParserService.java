package org.jmouse.expression;

import org.jmouse.common.ast.compiler.EvaluationContext;
import org.jmouse.common.ast.compiler.EvaluationContextFactory;
import org.jmouse.common.ast.lexer.Lexer;
import org.jmouse.common.ast.node.Node;
import org.jmouse.common.ast.node.RootNode;
import org.jmouse.common.ast.parser.Parser;
import org.jmouse.common.ast.parser.ParserContext;
import org.jmouse.common.ast.token.Token;
import org.jmouse.common.ast.token.Tokenizer;
import org.jmouse.expression.compiler.EvaluationContextConfigurator;
import org.jmouse.expression.parser.AnyExpressionParser;
import org.jmouse.expression.parser.ParserConfigurator;
import org.jmouse.common.support.resolver.Resolver;
import org.jmouse.common.support.resolver.ResolverContext;

import java.util.List;

// todo: consider to use Resolver interface
public class ParserService implements Resolver {

    private final ParserContext     parserContext     = new ParserContext.SimpleContext();
    private final EvaluationContext evaluationContext = EvaluationContextFactory.defaultEvaluationContext();
    private final Tokenizer         tokenizer         = new DefaultTokenizer();
    private       Parser            parser;

    public ParserService() {
        initialize();
    }

    private void initialize() {
        new EvaluationContextConfigurator().configure(evaluationContext);
        new ParserConfigurator().configure(parserContext);
        new TokenizerConfigurator().configure(tokenizer);

        this.parser = parserContext.getParser(AnyExpressionParser.class);
    }

    public <N extends Node> N parse(String inputString) {
        Node              root    = new RootNode();
        List<Token.Entry> entries = tokenizer.tokenize(inputString);
        Lexer             lexer   = new DefaultLexer(entries);

        this.parser.parse(lexer, root, parserContext);

        return (N) root.first();
    }

    public <T> T compile(Node node, EvaluationContext context) {
        return (T) node.evaluate(context);
    }

    public EvaluationContext getEvaluationContext() {
        return evaluationContext;
    }

    @Override
    public Object resolve(Object value, ResolverContext context) {
        return null;
    }
}
