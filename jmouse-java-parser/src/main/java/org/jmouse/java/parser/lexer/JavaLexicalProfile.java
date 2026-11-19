package org.jmouse.java.parser.lexer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.IntPredicate;
import org.jmouse.el.lexer.recognizer.OrderedRecognizerChain;
import org.jmouse.el.lexer.recognizer.Recognizer;

/** A frozen lexical vocabulary and recognition order, with replaceable Unicode identifier rules. */
public final class JavaLexicalProfile {
    private final JavaKeywordCatalog keywords;
    private final List<JavaLexicalRule> rules;
    private final Recognizer<JavaLexicalMatch, JavaLexicalInput> recognizer;
    private final IntPredicate identifierStart;
    private final IntPredicate identifierPart;

    public JavaLexicalProfile(JavaKeywordCatalog keywords, List<JavaLexicalRule> rules,
                               IntPredicate identifierStart, IntPredicate identifierPart) {
        this.keywords = Objects.requireNonNull(keywords, "keywords");
        this.rules = List.copyOf(rules);
        this.identifierStart = Objects.requireNonNull(identifierStart, "identifierStart");
        this.identifierPart = Objects.requireNonNull(identifierPart, "identifierPart");
        recognizer = new OrderedRecognizerChain<JavaLexicalMatch, JavaLexicalInput>(this.rules).snapshot();
    }

    public static JavaLexicalProfile standard() {
        return Standard.INSTANCE;
    }

    public JavaKeywordCatalog keywords() {
        return keywords;
    }

    public Optional<JavaLexicalMatch> recognize(JavaLexicalInput input) {
        return recognizer.recognize(input);
    }

    public boolean isIdentifierStart(int codePoint) {
        return identifierStart.test(codePoint);
    }

    public boolean isIdentifierPart(int codePoint) {
        return identifierPart.test(codePoint);
    }

    public JavaLexicalProfile withKeywords(Collection<? extends JavaKeywordDefinition> additions) {
        return new JavaLexicalProfile(keywords.extend(additions), rules, identifierStart, identifierPart);
    }

    public JavaLexicalProfile replaceKeyword(JavaKeywordDefinition expected, JavaKeywordDefinition replacement) {
        return new JavaLexicalProfile(keywords.replace(expected, replacement), rules, identifierStart, identifierPart);
    }

    public JavaLexicalProfile withRule(JavaLexicalRule rule) {
        List<JavaLexicalRule> extended = new ArrayList<>(rules);
        extended.add(rule);
        return new JavaLexicalProfile(keywords, extended, identifierStart, identifierPart);
    }

    public JavaLexicalProfile withIdentifiers(IntPredicate start, IntPredicate part) {
        return new JavaLexicalProfile(keywords, rules, start, part);
    }

    private static final class Standard {
        private static final JavaLexicalProfile INSTANCE = new JavaLexicalProfile(JavaKeywordCatalog.standard(), List.of(),
                Character::isJavaIdentifierStart, Character::isJavaIdentifierPart);
    }
}
