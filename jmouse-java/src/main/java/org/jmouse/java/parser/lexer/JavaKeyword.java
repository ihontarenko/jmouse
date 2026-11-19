package org.jmouse.java.parser.lexer;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.jmouse.java.model.version.JavaFeatureId;
import org.jmouse.java.model.version.JavaSourceFeatures;

/** Keyword spellings are declared once; contextual words remain identifier tokens until grammar admission. */
public enum JavaKeyword implements JavaKeywordDefinition {
    ABSTRACT(JavaKeywordSpellings.ABSTRACT, JavaKeywordCategory.RESERVED),
    ASSERT(JavaKeywordSpellings.ASSERT, JavaKeywordCategory.RESERVED),
    BOOLEAN(JavaKeywordSpellings.BOOLEAN, JavaKeywordCategory.RESERVED),
    BREAK(JavaKeywordSpellings.BREAK, JavaKeywordCategory.RESERVED),
    BYTE(JavaKeywordSpellings.BYTE, JavaKeywordCategory.RESERVED),
    CASE(JavaKeywordSpellings.CASE, JavaKeywordCategory.RESERVED),
    CATCH(JavaKeywordSpellings.CATCH, JavaKeywordCategory.RESERVED),
    CHAR(JavaKeywordSpellings.CHAR, JavaKeywordCategory.RESERVED),
    CLASS(JavaKeywordSpellings.CLASS, JavaKeywordCategory.RESERVED),
    CONST(JavaKeywordSpellings.CONST, JavaKeywordCategory.RESERVED),
    CONTINUE(JavaKeywordSpellings.CONTINUE, JavaKeywordCategory.RESERVED),
    DEFAULT(JavaKeywordSpellings.DEFAULT, JavaKeywordCategory.RESERVED),
    DO(JavaKeywordSpellings.DO, JavaKeywordCategory.RESERVED),
    DOUBLE(JavaKeywordSpellings.DOUBLE, JavaKeywordCategory.RESERVED),
    ELSE(JavaKeywordSpellings.ELSE, JavaKeywordCategory.RESERVED),
    ENUM(JavaKeywordSpellings.ENUM, JavaKeywordCategory.RESERVED),
    EXTENDS(JavaKeywordSpellings.EXTENDS, JavaKeywordCategory.RESERVED),
    FINAL(JavaKeywordSpellings.FINAL, JavaKeywordCategory.RESERVED),
    FINALLY(JavaKeywordSpellings.FINALLY, JavaKeywordCategory.RESERVED),
    FLOAT(JavaKeywordSpellings.FLOAT, JavaKeywordCategory.RESERVED),
    FOR(JavaKeywordSpellings.FOR, JavaKeywordCategory.RESERVED),
    GOTO(JavaKeywordSpellings.GOTO, JavaKeywordCategory.RESERVED),
    IF(JavaKeywordSpellings.IF, JavaKeywordCategory.RESERVED),
    IMPLEMENTS(JavaKeywordSpellings.IMPLEMENTS, JavaKeywordCategory.RESERVED),
    IMPORT(JavaKeywordSpellings.IMPORT, JavaKeywordCategory.RESERVED),
    INSTANCEOF(JavaKeywordSpellings.INSTANCEOF, JavaKeywordCategory.RESERVED),
    INT(JavaKeywordSpellings.INT, JavaKeywordCategory.RESERVED),
    INTERFACE(JavaKeywordSpellings.INTERFACE, JavaKeywordCategory.RESERVED),
    LONG(JavaKeywordSpellings.LONG, JavaKeywordCategory.RESERVED),
    NATIVE(JavaKeywordSpellings.NATIVE, JavaKeywordCategory.RESERVED),
    NEW(JavaKeywordSpellings.NEW, JavaKeywordCategory.RESERVED),
    PACKAGE(JavaKeywordSpellings.PACKAGE, JavaKeywordCategory.RESERVED),
    PRIVATE(JavaKeywordSpellings.PRIVATE, JavaKeywordCategory.RESERVED),
    PROTECTED(JavaKeywordSpellings.PROTECTED, JavaKeywordCategory.RESERVED),
    PUBLIC(JavaKeywordSpellings.PUBLIC, JavaKeywordCategory.RESERVED),
    RETURN(JavaKeywordSpellings.RETURN, JavaKeywordCategory.RESERVED),
    SHORT(JavaKeywordSpellings.SHORT, JavaKeywordCategory.RESERVED),
    STATIC(JavaKeywordSpellings.STATIC, JavaKeywordCategory.RESERVED),
    STRICTFP(JavaKeywordSpellings.STRICTFP, JavaKeywordCategory.RESERVED),
    SUPER(JavaKeywordSpellings.SUPER, JavaKeywordCategory.RESERVED),
    SWITCH(JavaKeywordSpellings.SWITCH, JavaKeywordCategory.RESERVED),
    SYNCHRONIZED(JavaKeywordSpellings.SYNCHRONIZED, JavaKeywordCategory.RESERVED),
    THIS(JavaKeywordSpellings.THIS, JavaKeywordCategory.RESERVED),
    THROW(JavaKeywordSpellings.THROW, JavaKeywordCategory.RESERVED),
    THROWS(JavaKeywordSpellings.THROWS, JavaKeywordCategory.RESERVED),
    TRANSIENT(JavaKeywordSpellings.TRANSIENT, JavaKeywordCategory.RESERVED),
    TRY(JavaKeywordSpellings.TRY, JavaKeywordCategory.RESERVED),
    VOID(JavaKeywordSpellings.VOID, JavaKeywordCategory.RESERVED),
    VOLATILE(JavaKeywordSpellings.VOLATILE, JavaKeywordCategory.RESERVED),
    WHILE(JavaKeywordSpellings.WHILE, JavaKeywordCategory.RESERVED),
    EXPORTS(JavaKeywordSpellings.EXPORTS, JavaKeywordCategory.CONTEXTUAL),
    MODULE(JavaKeywordSpellings.MODULE, JavaKeywordCategory.CONTEXTUAL),
    OPEN(JavaKeywordSpellings.OPEN, JavaKeywordCategory.CONTEXTUAL),
    OPENS(JavaKeywordSpellings.OPENS, JavaKeywordCategory.CONTEXTUAL),
    PROVIDES(JavaKeywordSpellings.PROVIDES, JavaKeywordCategory.CONTEXTUAL),
    RECORD(JavaKeywordSpellings.RECORD, JavaKeywordCategory.CONTEXTUAL),
    REQUIRES(JavaKeywordSpellings.REQUIRES, JavaKeywordCategory.CONTEXTUAL),
    SEALED(JavaKeywordSpellings.SEALED, JavaKeywordCategory.CONTEXTUAL),
    PERMITS(JavaKeywordSpellings.PERMITS, JavaKeywordCategory.CONTEXTUAL),
    NON_SEALED(JavaKeywordSpellings.NON_SEALED, JavaKeywordCategory.CONTEXTUAL),
    TO(JavaKeywordSpellings.TO, JavaKeywordCategory.CONTEXTUAL),
    TRANSITIVE(JavaKeywordSpellings.TRANSITIVE, JavaKeywordCategory.CONTEXTUAL),
    USES(JavaKeywordSpellings.USES, JavaKeywordCategory.CONTEXTUAL),
    VAR(JavaKeywordSpellings.VAR, JavaKeywordCategory.CONTEXTUAL),
    WHEN(JavaKeywordSpellings.WHEN, JavaKeywordCategory.CONTEXTUAL),
    WITH(JavaKeywordSpellings.WITH, JavaKeywordCategory.CONTEXTUAL),
    YIELD(JavaKeywordSpellings.YIELD, JavaKeywordCategory.CONTEXTUAL),
    UNDERSCORE(JavaKeywordSpellings.UNDERSCORE, JavaKeywordCategory.VERSIONED_RESERVED);


    private static final Map<String, JavaKeyword> BY_SPELLING = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(JavaKeyword::spelling, Function.identity()));

    private final String spelling;
    private final JavaKeywordCategory category;

    JavaKeyword(String spelling, JavaKeywordCategory category) {
        this.spelling = spelling;
        this.category = category;
    }

    public String spelling() {
        return spelling;
    }

    public JavaKeywordCategory category() {
        return category;
    }

    public Optional<JavaFeatureId> typeNameRestriction() {
        return switch (this) {
            case VAR -> Optional.of(JavaSourceFeatures.LOCAL_VARIABLE_INFERENCE);
            case YIELD -> Optional.of(JavaSourceFeatures.SWITCH_EXPRESSIONS);
            case RECORD -> Optional.of(JavaSourceFeatures.RECORD_DECLARATIONS);
            case SEALED, PERMITS -> Optional.of(JavaSourceFeatures.SEALED_TYPES);
            default -> Optional.empty();
        };
    }

    public static Optional<JavaKeyword> fromSpelling(String spelling) {
        return Optional.ofNullable(BY_SPELLING.get(spelling));
    }

    public static JavaKeyword require(String spelling) {
        return fromSpelling(spelling).orElseThrow(
                () -> new IllegalArgumentException("Unknown Java keyword: " + spelling));
    }

    public static boolean isReserved(String spelling) {
        JavaKeyword keyword = BY_SPELLING.get(spelling);
        return keyword != null && keyword.category == JavaKeywordCategory.RESERVED;
    }
}
