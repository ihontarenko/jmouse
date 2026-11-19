package org.jmouse.java.parser.lexer;

import org.jmouse.el.lexer.Cursor;
import org.jmouse.el.lexer.Lexer;
import org.jmouse.el.lexer.TokenCursor;
import java.util.Objects;
import org.jmouse.java.model.version.JavaSourceProfile;
import org.jmouse.java.model.version.JavaSpecificationCatalog;

/**
 * The jME {@link Lexer} for Java: a text in, a {@link TokenCursor} over its tokens out.
 *
 * <p>The cursor's {@link TokenCursor#source() source} is always a {@link JavaSourceText}; that is where
 * the comments, lexical problems, token end offsets and line index are read from. Pass a
 * {@code JavaSourceText} to choose its name; any other text is wrapped under the name {@code <text>}.
 *
 * <p>Instances are stateless and safe to share.
 */
public final class JavaLexer implements Lexer {

    private final JavaTokenizer tokenizer;

    public JavaLexer() {
        this(JavaSourceProfile.standard(JavaSpecificationCatalog.standard().defaultRelease()), JavaLexicalProfile.standard());
    }

    public JavaLexer(JavaSourceProfile profile, JavaLexicalProfile lexical) {
        tokenizer = new JavaTokenizer(Objects.requireNonNull(profile, "profile"), Objects.requireNonNull(lexical, "lexical"));
    }

    @Override
    public TokenCursor tokenize(CharSequence text) {
        JavaSourceText source = text instanceof JavaSourceText javaSource ? javaSource : new JavaSourceText("<text>", text);
        return new Cursor(source, tokenizer.tokenize(source));
    }

    /**
     * Returns the Java source behind a cursor this lexer produced.
     *
     * @param cursor the cursor
     * @return its source
     */
    public static JavaSourceText sourceOf(TokenCursor cursor) {
        if (cursor.source() instanceof JavaSourceText source) {
            return source;
        }
        throw new IllegalArgumentException("The cursor was not produced by the Java lexer.");
    }
}
