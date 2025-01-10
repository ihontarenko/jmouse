package svit.expression;

import svit.ast.compiler.EvaluationContext;
import svit.ast.compiler.EvaluationContextFactory;
import svit.ast.lexer.Lexer;
import svit.ast.node.Node;
import svit.ast.node.RootNode;
import svit.ast.parser.Parser;
import svit.ast.parser.ParserContext;
import svit.ast.token.Token;
import svit.ast.token.Tokenizer;
import svit.expression.compiler.EvaluationContextConfigurator;
import svit.expression.parser.AnyExpressionParser;
import svit.expression.parser.ParserConfigurator;
import svit.resolver.Resolver;
import svit.resolver.ResolverContext;

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
