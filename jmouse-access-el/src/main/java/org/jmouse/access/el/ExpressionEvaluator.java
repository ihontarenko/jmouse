package org.jmouse.access.el;

import org.jmouse.access.el.lexer.AccessRecognizer;
import org.jmouse.access.el.node.PolicyNode;
import org.jmouse.access.el.parser.PolicyDocumentParser;
import org.jmouse.access.policy.model.PolicyDocument;
import org.jmouse.access.policy.model.SourceSpan;
import org.jmouse.el.ExpressionLanguage;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.extension.ExtensionContainer;
import org.jmouse.el.extension.StandardExtensionContainer;
import org.jmouse.el.lexer.DefaultLexer;
import org.jmouse.el.lexer.DefaultTokenizer;
import org.jmouse.el.lexer.ExpressionSplitter;

import java.util.Map;

/**
 * Reads {@code .jmp} source and returns the document it describes.
 *
 * <p>This is the whole of stage 1's public surface. What comes back is a {@link PolicyDocument} of
 * plain strings: no scope, no permission, no compiled condition, nothing that knows a catalogue
 * exists. Everything that can be <em>wrong about the model</em> — an unregistered scope, a
 * permission nobody declared, a condition that will not compile — belongs to the binder, so that a
 * parser failure and a policy failure never look alike to whoever has to read the message.</p>
 *
 * <pre>{@code
 * PolicyDocument document = new ExpressionEvaluator().parse(source);
 * AccessPolicy   policy   = new PolicyBinder(scopes, permissions, conditions).bind(document);
 * GrantStore     store    = new PolicyGrantStore(policy);
 * }</pre>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
final public class ExpressionEvaluator {

    private final ExpressionLanguage expressionLanguage;

    /** What an unnamed file is called, where the caller offered no better name. */
    private static final String ANONYMOUS = "anonymous";

    public ExpressionEvaluator() {
        this(new ExpressionLanguage(
                defaultExtensions(),
                new DefaultLexer(new DefaultTokenizer(new ExpressionSplitter(), new AccessRecognizer())),
                PolicyDocumentParser.class
        ));
    }

    public ExpressionEvaluator(ExpressionLanguage expressionLanguage) {
        this.expressionLanguage = expressionLanguage;
    }

    /**
     * Parses a policy file that names itself, or is content to be called {@code anonymous}.
     *
     * @param source the file's text
     * @return the document it describes
     * @throws PolicyParseException when the text is not a policy
     */
    public PolicyDocument parse(String source) {
        return parse(source, ANONYMOUS);
    }

    /**
     * Parses a policy file, naming it where its own text does not.
     *
     * <p>A {@code policy "…"} header wins: a file that named itself keeps that name however it was
     * loaded, so provenance reads the same whether the loader found it on the classpath or somebody
     * pasted it into the control room.</p>
     *
     * @param source       the file's text
     * @param documentName what to call it when it carries no header — usually the file name
     * @return the document it describes
     * @throws PolicyParseException when the text is not a policy
     */
    public PolicyDocument parse(String source, String documentName) {
        Object result = evaluate(source, Map.of());

        if (result instanceof PolicyDocument document) {
            return document.name() == null ? rename(document, documentName) : document;
        }

        throw new PolicyParseException(
                SourceSpan.none(),
                "this is not a policy file; expected a 'policy', 'role' or 'subject' declaration"
        );
    }

    /**
     * Parses a policy file and writes it back out.
     *
     * <p>What comes back is the same policy in the language's own spelling — one statement per line,
     * one indent per block, names quoted exactly where the lexer needs them. Two properties make it
     * worth having: the control room can show an administrator a file for a policy that never was
     * one, and rewriting an already-rewritten file changes nothing, which is what makes the output
     * safe to diff.</p>
     *
     * <p>⚠️ Comments do not survive. They are not part of what the file declares and the parser does
     * not keep them, so a rewrite is a rendering of the policy rather than an edit of the text.</p>
     *
     * @param source the file's text
     * @return the same policy, normalised
     * @throws PolicyParseException when the text is not a policy
     */
    public String rewrite(String source) {
        if (expressionLanguage.compile(source) instanceof PolicyNode document) {
            return document.toSource();
        }

        throw new PolicyParseException(
                SourceSpan.none(),
                "this is not a policy file; expected a 'policy', 'role' or 'subject' declaration"
        );
    }

    /**
     * Evaluates source against this language, for callers that want the raw result.
     *
     * @param code      the source to evaluate
     * @param variables variables the source may read
     * @return whatever the source evaluated to
     */
    public Object evaluate(String code, Map<String, Object> variables) {
        EvaluationContext context = expressionLanguage.newContext();
        return expressionLanguage.evaluate(code, context, variables, Object.class);
    }

    /**
     * Returns the same document under a name it did not choose for itself.
     *
     * @param document the parsed document
     * @param name     what to call it
     * @return a copy carrying the name
     */
    private static PolicyDocument rename(PolicyDocument document, String name) {
        return new PolicyDocument(
                name,
                document.includes(),
                document.scopes(),
                document.permissions(),
                document.capabilities(),
                document.roles(),
                document.plans(),
                document.subjects(),
                document.entitlements()
        );
    }

    private static ExtensionContainer defaultExtensions() {
        StandardExtensionContainer container = new StandardExtensionContainer();
        container.importExtension(new ExpressionExtension());
        return container;
    }

}
