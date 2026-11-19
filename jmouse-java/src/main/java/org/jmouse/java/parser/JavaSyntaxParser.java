package org.jmouse.java.parser;

import org.jmouse.el.extension.StandardExtensionContainer;
import org.jmouse.el.grammar.definition.GrammarDefinition;
import org.jmouse.el.parser.ParseRole;
import org.jmouse.el.parser.ParseRuleRegistry;
import org.jmouse.el.parser.ParsingBudget;
import org.jmouse.java.model.version.JavaSourceProfile;
import org.jmouse.java.parser.extension.JavaSourceLanguage;
import org.jmouse.java.parser.grammar.JavaAnnotationParser;
import org.jmouse.java.parser.grammar.JavaAnnotationValueParser;
import org.jmouse.java.parser.grammar.JavaArrayInitializerParser;
import org.jmouse.java.parser.grammar.JavaCastExpressionParser;
import org.jmouse.java.parser.grammar.JavaConditionalExpressionParser;
import org.jmouse.java.parser.grammar.JavaConstructorInvocationParser;
import org.jmouse.java.parser.grammar.JavaCreationExpressionParser;
import org.jmouse.java.parser.grammar.JavaExpressionParser;
import org.jmouse.java.parser.grammar.JavaLambdaExpressionParser;
import org.jmouse.java.parser.grammar.JavaLiteralParser;
import org.jmouse.java.parser.grammar.JavaModifiersParser;
import org.jmouse.java.parser.grammar.JavaOperatorCatalog;
import org.jmouse.java.parser.grammar.JavaParameterParser;
import org.jmouse.java.parser.grammar.JavaParseRoles;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.grammar.JavaParsingMode;
import org.jmouse.java.parser.grammar.JavaPatternParser;
import org.jmouse.java.parser.grammar.JavaPrimaryExpressionParser;
import org.jmouse.java.parser.grammar.JavaQualifiedNameParser;
import org.jmouse.java.parser.grammar.JavaReceiverParameterParser;
import org.jmouse.java.parser.grammar.JavaRecoveryPolicy;
import org.jmouse.java.parser.grammar.JavaReferenceTypeParser;
import org.jmouse.java.parser.grammar.JavaSwitchExpressionParser;
import org.jmouse.java.parser.grammar.JavaTriviaAttachment;
import org.jmouse.java.parser.grammar.JavaTypeArgumentParser;
import org.jmouse.java.parser.grammar.JavaTypeParser;
import org.jmouse.java.parser.grammar.JavaVariableInitializerParser;
import org.jmouse.java.parser.grammar.declaration.JavaDeclarationGrammar;
import org.jmouse.java.parser.grammar.module.JavaModuleGrammar;
import org.jmouse.java.parser.grammar.statement.JavaStatementGrammar;
import org.jmouse.java.parser.grammar.statement.JavaStatementRoles;
import org.jmouse.java.parser.grammar.unit.JavaCompilationUnitParser;
import org.jmouse.java.parser.grammar.unit.JavaImportDeclarationParser;
import org.jmouse.java.parser.grammar.unit.JavaPackageDeclarationParser;
import org.jmouse.java.parser.grammar.unit.JavaUnitRoles;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.lexer.JavaLexicalProfile;
import org.jmouse.java.parser.lexer.JavaLexicalSnapshot;
import org.jmouse.java.parser.lexer.JavaSourceText;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.node.expression.JavaExpressionNode;
import org.jmouse.java.parser.node.statement.JavaStatementNode;
import org.jmouse.java.parser.node.type.JavaTypeSyntaxNode;
import org.jmouse.java.parser.node.unit.JavaCompilationUnitNode;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/** Configurable source grammar facade. Each invocation has isolated jME input and session state. */
public final class JavaSyntaxParser {
    private static final Properties DEFAULTS = loadDefaults();
    private final JavaSourceProfile profile;
    private final ParsingBudget budget;
    private final ParseRuleRegistry rules;
    private final JavaSourceLanguage language;

    public JavaSyntaxParser() {
        this(JavaSourceLanguage.standard(), defaultBudget());
    }

    public JavaSyntaxParser(
            JavaSourceProfile profile, ParsingBudget budget, ParseRuleRegistry rules) {
        this(new JavaSourceLanguage(profile, JavaLexicalProfile.standard(), rules), budget);
    }

    public JavaSyntaxParser(JavaSourceLanguage language, ParsingBudget budget) {
        this.language = Objects.requireNonNull(language, "language");
        this.profile = language.profile();
        this.budget = Objects.requireNonNull(budget, "budget");
        this.rules = language.rules();
    }

    public JavaExpressionNode parseExpression(String sourceName, CharSequence text) {
        return parse(JavaParseRoles.EXPRESSION, sourceName, text);
    }

    public JavaTypeSyntaxNode parseType(String sourceName, CharSequence text) {
        return parse(JavaParseRoles.TYPE, sourceName, text);
    }

    public JavaStatementNode parseStatement(String sourceName, CharSequence text) {
        return parse(JavaStatementRoles.STATEMENT, sourceName, text);
    }

    public JavaCompilationUnitNode parseCompilationUnit(String sourceName, CharSequence text) {
        return parse(JavaUnitRoles.COMPILATION_UNIT, sourceName, text);
    }

    public JavaSyntaxResult<JavaCompilationUnitNode> recoverCompilationUnit(
            String sourceName, CharSequence text) {
        return parseSource(
                JavaUnitRoles.COMPILATION_UNIT, sourceName, text, JavaRecoveryPolicy.RECOVER);
    }

    public <N extends JavaSyntaxNode> N parse(
            ParseRole<JavaParsingInput, N> entry, String sourceName, CharSequence text) {
        return parseSource(entry, sourceName, text).root();
    }

    public <N extends JavaSyntaxNode> JavaSyntaxResult<N> parseSource(
            ParseRole<JavaParsingInput, N> entry, String sourceName, CharSequence text) {
        return parseSource(entry, sourceName, text, JavaRecoveryPolicy.STRICT);
    }

    public <N extends JavaSyntaxNode> JavaSyntaxResult<N> parseSource(
            ParseRole<JavaParsingInput, N> entry,
            String sourceName,
            CharSequence text,
            JavaRecoveryPolicy recovery) {
        return parseSource(entry, sourceName, text, JavaParsingMode.COMPLETE, recovery);
    }

    public <N extends JavaSyntaxNode> JavaSyntaxResult<N> parseSource(
            ParseRole<JavaParsingInput, N> entry,
            String sourceName,
            CharSequence text,
            JavaParsingMode mode,
            JavaRecoveryPolicy recovery) {
        JavaParsingInput input =
                new JavaParsingInput(
                        language.newLexer().tokenize(new JavaSourceText(sourceName, text)),
                        profile,
                        recovery,
                        language.lexical().keywords(),
                        mode);
        GrammarDefinition<JavaParsingInput, N> grammar =
                new GrammarDefinition<>(entry, rules, List.of());
        N root = grammar.parse(input, budget);
        input.requireEnd();
        JavaLexicalSnapshot source = new JavaLexicalSnapshot(input.source());
        JavaTriviaAttachment.attach(root, source);
        root.freeze();
        return new JavaSyntaxResult<>(
                root, source, profile, input.problems(), mode, input.omittedRegions());
    }

    public ParseRuleRegistry bindings() {
        return rules;
    }

    public static ParseRuleRegistry standardRules() {
        ParseRuleRegistry rules = new StandardExtensionContainer().parseRules();
        JavaOperatorCatalog operators = JavaOperatorCatalog.standard();
        JavaDeclarationGrammar.install(rules);
        JavaModuleGrammar.install(rules);
        rules.bind(
                JavaParseRoles.ANNOTATION_VALUE,
                JavaAnnotationValueParser.class.getName(),
                new JavaAnnotationValueParser());
        rules.bind(
                JavaParseRoles.TYPE_ARGUMENT,
                JavaTypeArgumentParser.class.getName(),
                new JavaTypeArgumentParser());
        rules.bind(
                JavaParseRoles.REFERENCE_TYPE,
                JavaReferenceTypeParser.class.getName(),
                new JavaReferenceTypeParser());
        rules.bind(
                JavaUnitRoles.COMPILATION_UNIT,
                JavaCompilationUnitParser.class.getName(),
                new JavaCompilationUnitParser());
        rules.bind(
                JavaUnitRoles.IMPORT,
                JavaImportDeclarationParser.class.getName(),
                new JavaImportDeclarationParser());
        rules.bind(
                JavaUnitRoles.PACKAGE,
                JavaPackageDeclarationParser.class.getName(),
                new JavaPackageDeclarationParser());
        rules.bind(
                JavaParseRoles.CONSTRUCTOR_INVOCATION,
                JavaConstructorInvocationParser.class.getName(),
                new JavaConstructorInvocationParser());
        rules.bind(
                JavaParseRoles.RECEIVER_PARAMETER,
                JavaReceiverParameterParser.class.getName(),
                new JavaReceiverParameterParser());
        JavaStatementGrammar.install(rules, DEFAULTS);
        rules.bind(
                JavaParseRoles.SWITCH_EXPRESSION,
                JavaSwitchExpressionParser.class.getName(),
                new JavaSwitchExpressionParser());
        rules.bind(
                JavaParseRoles.PRIMARY,
                JavaPrimaryExpressionParser.class.getName(),
                new JavaPrimaryExpressionParser(operators));
        rules.bind(
                JavaParseRoles.PARAMETER,
                JavaParameterParser.class.getName(),
                new JavaParameterParser());
        rules.bind(
                JavaParseRoles.LAMBDA,
                JavaLambdaExpressionParser.class.getName(),
                new JavaLambdaExpressionParser());
        rules.bind(
                JavaParseRoles.CREATION,
                JavaCreationExpressionParser.class.getName(),
                new JavaCreationExpressionParser());
        rules.bind(
                JavaParseRoles.ARRAY_INITIALIZER,
                JavaArrayInitializerParser.class.getName(),
                new JavaArrayInitializerParser());
        rules.bind(
                JavaParseRoles.VARIABLE_INITIALIZER,
                JavaVariableInitializerParser.class.getName(),
                new JavaVariableInitializerParser());
        rules.bind(
                JavaParseRoles.MODIFIERS,
                JavaModifiersParser.class.getName(),
                new JavaModifiersParser(
                        Arrays.stream(DEFAULTS.getProperty("declaration.modifiers").split(","))
                                .map(JavaKeyword::require)
                                .collect(Collectors.toUnmodifiableSet())));
        rules.bind(
                JavaParseRoles.VARIABLE_MODIFIERS,
                JavaModifiersParser.class.getName(),
                new JavaModifiersParser(Set.of(JavaKeyword.FINAL)));
        rules.bind(
                JavaParseRoles.PATTERN, JavaPatternParser.class.getName(), new JavaPatternParser());
        rules.bind(
                JavaParseRoles.CAST,
                JavaCastExpressionParser.class.getName(),
                new JavaCastExpressionParser());
        rules.bind(
                JavaParseRoles.UNARY_EXPRESSION,
                JavaExpressionParser.class.getName(),
                new JavaExpressionParser(
                        operators,
                        integer("expression.prefix-precedence"),
                        integer("expression.prefix-precedence")));
        rules.bind(JavaParseRoles.TYPE, JavaTypeParser.class.getName(), new JavaTypeParser());
        rules.bind(
                JavaParseRoles.ANNOTATION,
                JavaAnnotationParser.class.getName(),
                new JavaAnnotationParser());
        rules.bind(
                JavaParseRoles.QUALIFIED_NAME,
                JavaQualifiedNameParser.class.getName(),
                new JavaQualifiedNameParser());
        rules.bind(
                JavaParseRoles.LITERAL, JavaLiteralParser.class.getName(), new JavaLiteralParser());
        rules.bind(
                JavaParseRoles.EXPRESSION,
                JavaExpressionParser.class.getName(),
                new JavaExpressionParser(
                        operators,
                        integer("expression.minimum-precedence"),
                        integer("expression.prefix-precedence")));
        rules.bind(
                JavaParseRoles.CONDITIONAL_EXPRESSION,
                JavaConditionalExpressionParser.class.getName(),
                new JavaConditionalExpressionParser(
                        operators,
                        integer("expression.conditional-precedence"),
                        integer("expression.prefix-precedence")));
        return rules;
    }

    public static ParsingBudget defaultBudget() {
        return new ParsingBudget(
                integer("budget.maximum-depth"),
                Long.parseLong(DEFAULTS.getProperty("budget.maximum-steps")));
    }

    private static int integer(String key) {
        return Integer.parseInt(Objects.requireNonNull(DEFAULTS.getProperty(key), key));
    }

    private static Properties loadDefaults() {
        Properties properties = new Properties();
        try (InputStream stream =
                JavaSyntaxParser.class.getResourceAsStream(
                        "/org/jmouse/java/parser/source-parser.properties")) {
            if (stream == null) {
                throw new IllegalStateException("Missing Java source parser defaults");
            }
            properties.load(stream);
            return properties;
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read Java parser defaults", exception);
        }
    }
}
