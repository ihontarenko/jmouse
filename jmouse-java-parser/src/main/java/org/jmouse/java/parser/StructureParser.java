package org.jmouse.java.parser;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.el.node.Trivia;
import org.jmouse.java.parser.javadoc.JavaDocParser;
import org.jmouse.java.parser.lexer.JavaComment;
import org.jmouse.java.parser.lexer.JavaLexer;
import org.jmouse.java.parser.lexer.JavaSourceText;
import org.jmouse.java.parser.lexer.JavaTokenKind;
import org.jmouse.java.parser.model.Annotation;
import org.jmouse.java.parser.model.ConstructorDeclaration;
import org.jmouse.java.parser.model.DeclarationKind;
import org.jmouse.java.parser.model.EnumConstantDeclaration;
import org.jmouse.java.parser.model.FieldDeclaration;
import org.jmouse.java.parser.model.ImportDeclaration;
import org.jmouse.java.parser.model.InitializerDeclaration;
import org.jmouse.java.parser.model.JavaDoc;
import org.jmouse.java.parser.model.JavaDocOwner;
import org.jmouse.java.parser.model.MethodDeclaration;
import org.jmouse.java.parser.model.Modifier;
import org.jmouse.java.parser.model.ModuleDeclaration;
import org.jmouse.java.parser.model.PackageDeclaration;
import org.jmouse.java.parser.model.Parameter;
import org.jmouse.java.parser.model.RecordComponentDeclaration;
import org.jmouse.java.parser.model.SymbolId;
import org.jmouse.java.parser.model.TypeDeclaration;
import org.jmouse.java.parser.model.TypeParameter;
import org.jmouse.java.parser.node.CompilationUnitNode;
import org.jmouse.java.parser.node.ImportNode;
import org.jmouse.java.parser.node.JavaNode;
import org.jmouse.java.parser.node.MemberNode;
import org.jmouse.java.parser.node.ModuleNode;
import org.jmouse.java.parser.node.PackageNode;
import org.jmouse.java.parser.node.TypeNode;
import org.jmouse.java.parser.source.LineIndex;
import org.jmouse.java.parser.source.ParseProblem;
import org.jmouse.java.parser.source.ProblemKind;
import org.jmouse.java.parser.source.SourceSpan;

import static org.jmouse.java.parser.lexer.JavaTokenKind.AMPERSAND;
import static org.jmouse.java.parser.lexer.JavaTokenKind.ASSIGN;
import static org.jmouse.java.parser.lexer.JavaTokenKind.AT;
import static org.jmouse.java.parser.lexer.JavaTokenKind.COMMA;
import static org.jmouse.java.parser.lexer.JavaTokenKind.DOT;
import static org.jmouse.java.parser.lexer.JavaTokenKind.ELLIPSIS;
import static org.jmouse.java.parser.lexer.JavaTokenKind.END_OF_FILE;
import static org.jmouse.java.parser.lexer.JavaTokenKind.GREATER;
import static org.jmouse.java.parser.lexer.JavaTokenKind.IDENTIFIER;
import static org.jmouse.java.parser.lexer.JavaTokenKind.KEYWORD;
import static org.jmouse.java.parser.lexer.JavaTokenKind.LEFT_BRACE;
import static org.jmouse.java.parser.lexer.JavaTokenKind.LEFT_BRACKET;
import static org.jmouse.java.parser.lexer.JavaTokenKind.LEFT_PARENTHESIS;
import static org.jmouse.java.parser.lexer.JavaTokenKind.LESS;
import static org.jmouse.java.parser.lexer.JavaTokenKind.OPERATOR;
import static org.jmouse.java.parser.lexer.JavaTokenKind.QUESTION;
import static org.jmouse.java.parser.lexer.JavaTokenKind.RIGHT_BRACE;
import static org.jmouse.java.parser.lexer.JavaTokenKind.RIGHT_BRACKET;
import static org.jmouse.java.parser.lexer.JavaTokenKind.RIGHT_PARENTHESIS;
import static org.jmouse.java.parser.lexer.JavaTokenKind.SEMICOLON;
import static org.jmouse.java.parser.lexer.JavaTokenKind.isIdentifier;
import static org.jmouse.java.parser.lexer.JavaTokenKind.isKeyword;

/**
 * One recursive-descent pass over one jME {@link TokenCursor}, building a jME node tree. Not reusable:
 * create one per file.
 *
 * <p>Two kinds of reading are mixed on purpose. Declarations are parsed and consumed through the
 * cursor; types are first <em>scanned</em> by pure functions that look ahead without moving and return
 * the absolute position where a type would end, or {@code -1} — which is what lets an initializer's
 * {@code new HashMap<String, Integer>()} be told apart from the comma that separates two variables.
 *
 * <p>Recovery is per declaration: the loop over a type body (or over the file) takes a
 * {@link TokenCursor#savepoint() savepoint} before each declaration; a {@link SyntaxFailure} is caught,
 * reported, the cursor is restored to that savepoint, and parsing resumes after the failed
 * declaration's semicolon or closing brace.
 *
 * <p>The cursor is moved only by {@code next}, {@code savepoint} and {@code restore}; everything else
 * looks without moving, through {@code peek} and {@code lookAt}. {@code lookAt} is clamped at both ends
 * by jME's {@code Cursor}, so a look behind the first token or past the last one meets a boundary token
 * rather than an exception — which is what {@code tokenAt} and {@code endOfPrevious} rely on.
 */
final class StructureParser {

    private static final int MAXIMUM_TYPE_NESTING = 64;

    private static final Set<String> PRIMITIVE_TYPES = Set.of(
            "boolean", "byte", "char", "short", "int", "long", "float", "double", "void");

    private final TokenCursor          cursor;
    private final JavaSourceText       source;
    private final LineIndex            lines;
    private final List<JavaComment>    comments;
    private final JavaDocParser        javaDocParser;
    private final List<ParseProblem>   problems;
    private final List<JavaComment>    documentationComments;
    private final JavaDoc[]            parsedDocumentation;
    private final boolean[]            claimedDocumentation;
    private final List<JavaDoc>        javaDocs      = new ArrayList<>();
    private final List<SourceSpan>     opaqueBodies  = new ArrayList<>();
    private final Map<String, Integer> symbolCounts  = new HashMap<>();
    private       String               packageName   = "";

    StructureParser(TokenCursor cursor, JavaDocParser javaDocParser) {
        this.cursor = cursor;
        this.source = JavaLexer.sourceOf(cursor);
        this.lines = source.lines();
        this.comments = source.comments();
        this.javaDocParser = javaDocParser;
        this.problems = new ArrayList<>(source.problems());
        this.documentationComments = comments.stream()
                .filter(comment -> comment.kind() == JavaComment.Kind.DOCUMENTATION)
                .toList();
        this.parsedDocumentation = new JavaDoc[documentationComments.size()];
        this.claimedDocumentation = new boolean[documentationComments.size()];
    }

    CompilationUnitNode parse() {
        List<JavaNode> children = new ArrayList<>();

        while (!at(END_OF_FILE)) {
            TokenCursor.Savepoint start = cursor.savepoint();
            try {
                if (at(SEMICOLON)) {
                    advance();
                } else if (atKeyword("import")) {
                    int startIndex = position();
                    children.add(withTrivia(new ImportNode(parseImport()), startIndex));
                } else {
                    Header header = parseHeader();
                    if (atKeyword("package")) {
                        children.add(withTrivia(new PackageNode(parsePackage(header)), header.startIndex()));
                    } else if (isModuleStart()) {
                        children.add(withTrivia(new ModuleNode(parseModule(header)), header.startIndex()));
                    } else if (isTypeStart()) {
                        children.add(parseType(header, null, 0));
                    } else {
                        throw failure("Expected a package, import or type declaration.");
                    }
                }
            } catch (SyntaxFailure failure) {
                problems.add(failure.problem());
                recover(start, true);
            }
        }

        addOrphanDocumentation();
        javaDocs.sort(Comparator.comparingInt(javaDoc -> javaDoc.span().start().offset()));
        problems.sort(Comparator.comparingInt(problem -> problem.span().start().offset()));
        CompilationUnitNode root = new CompilationUnitNode(source.getName(), lines.span(0, source.length()), comments, javaDocs, problems);
        children.forEach(root::add);
        return root;
    }

    // ------------------------------------------------------------------ compilation unit

    private ImportDeclaration parseImport() {
        Token   keyword  = advance();
        boolean isStatic = false;
        if (atKeyword("static")) {
            advance();
            isStatic = true;
        }
        StringBuilder name     = new StringBuilder(expectIdentifier("an imported name").value());
        boolean       onDemand = false;
        while (at(DOT)) {
            advance();
            if (OPERATOR.matches(current()) && current().value().equals("*")) {
                advance();
                onDemand = true;
                break;
            }
            name.append('.').append(expectIdentifier("an imported name").value());
        }
        expect(SEMICOLON, "Expected ';' after an import.");
        return new ImportDeclaration(name.toString(), isStatic, onDemand, lines.span(keyword.offset(), endOfPrevious()));
    }

    private PackageDeclaration parsePackage(Header header) {
        advance();
        String name = parseQualifiedName("a package name");
        expect(SEMICOLON, "Expected ';' after the package name.");
        SourceSpan span = spanFrom(header.startIndex());
        packageName = name;
        JavaDoc javaDoc = documentationFor(header.startIndex(),
                new JavaDocOwner(new SymbolId(name), DeclarationKind.PACKAGE, name, "package " + name, span));
        return new PackageDeclaration(name, header.annotations(), span, javaDoc);
    }

    private boolean isModuleStart() {
        return isIdentifier(current(), "module") && IDENTIFIER.matches(peek(1))
                || isIdentifier(current(), "open") && isIdentifier(peek(1), "module");
    }

    private ModuleDeclaration parseModule(Header header) {
        boolean open = isIdentifier(current(), "open");
        if (open) {
            advance();
        }
        advance();
        String name = parseQualifiedName("a module name");
        if (!at(LEFT_BRACE)) {
            throw failure("Expected '{' to open the module body.");
        }
        String     signature = collapse(spanFrom(header.startIndex()));
        SourceSpan body      = skipBody();
        SourceSpan span      = spanFrom(header.startIndex());
        JavaDoc javaDoc = documentationFor(header.startIndex(),
                new JavaDocOwner(new SymbolId(name), DeclarationKind.MODULE, name, signature, span));
        return new ModuleDeclaration(name, open, header.annotations(), body, span, javaDoc);
    }

    // ------------------------------------------------------------------ headers and annotations

    /** Annotations and modifiers, in any order, with the absolute token position where they began. */
    private record Header(int startIndex, Set<Modifier> modifiers, List<Annotation> annotations) {
    }

    private Header parseHeader() {
        int              startIndex  = position();
        Set<Modifier>    modifiers   = EnumSet.noneOf(Modifier.class);
        List<Annotation> annotations = new ArrayList<>();

        while (true) {
            Token token = current();
            if (AT.matches(token) && !isKeyword(peek(1), "interface")) {
                annotations.add(parseAnnotation());
                continue;
            }
            Modifier keywordModifier = KEYWORD.matches(token) ? Modifier.ofKeyword(token.value()) : null;
            if (keywordModifier != null && !(keywordModifier == Modifier.STATIC && LEFT_BRACE.matches(peek(1)))) {
                modifiers.add(keywordModifier);
                advance();
                continue;
            }
            if (isIdentifier(token, "sealed") && continuesDeclaration(peek(1))) {
                modifiers.add(Modifier.SEALED);
                advance();
                continue;
            }
            if (isNonSealed()) {
                modifiers.add(Modifier.NON_SEALED);
                cursor.shift(3);
                continue;
            }
            return new Header(startIndex, modifiers, annotations);
        }
    }

    private boolean isNonSealed() {
        Token first  = current();
        Token hyphen = peek(1);
        Token second = peek(2);
        return isIdentifier(first, "non") && OPERATOR.matches(hyphen) && hyphen.value().equals("-") && isIdentifier(second, "sealed")
                && endOf(first) == hyphen.offset() && endOf(hyphen) == second.offset();
    }

    private static boolean continuesDeclaration(Token token) {
        return IDENTIFIER.matches(token) || KEYWORD.matches(token) || AT.matches(token);
    }

    private Annotation parseAnnotation() {
        Token  at        = advance();
        String name      = parseQualifiedName("an annotation name");
        String arguments = null;
        if (at(LEFT_PARENTHESIS)) {
            Token open  = current();
            Token close = skipBalanced(LEFT_PARENTHESIS, RIGHT_PARENTHESIS);
            int   end   = close == null ? current().offset() : close.offset();
            arguments = source.subSequence(endOf(open), end).toString();
        }
        return new Annotation(name, arguments, lines.span(at.offset(), endOfPrevious()));
    }

    private List<Annotation> parseAnnotations() {
        List<Annotation> annotations = new ArrayList<>();
        while (at(AT) && !isKeyword(peek(1), "interface")) {
            annotations.add(parseAnnotation());
        }
        return annotations;
    }

    private String parseQualifiedName(String expectation) {
        StringBuilder name = new StringBuilder(expectIdentifier(expectation).value());
        while (at(DOT) && IDENTIFIER.matches(peek(1))) {
            advance();
            name.append('.').append(advance().value());
        }
        return name.toString();
    }

    // ------------------------------------------------------------------ types

    private boolean isTypeStart() {
        return atKeyword("class") || atKeyword("interface") || atKeyword("enum")
                || at(AT) && isKeyword(peek(1), "interface")
                || isIdentifier(current(), "record") && IDENTIFIER.matches(peek(1));
    }

    /** What a member needs to know about the type it is declared in. */
    private static final class TypeContext {

        private final DeclarationKind kind;
        private final String          name;
        private final String          qualifiedName;
        private final List<String>    componentIdentityTypes;
        private       int             instanceInitializers;
        private       int             staticInitializers;

        private TypeContext(DeclarationKind kind, String name, String qualifiedName, List<String> componentIdentityTypes) {
            this.kind = kind;
            this.name = name;
            this.qualifiedName = qualifiedName;
            this.componentIdentityTypes = componentIdentityTypes;
        }

        private int nextInitializerOrdinal(boolean isStatic) {
            if (isStatic) {
                return ++staticInitializers;
            }
            return ++instanceInitializers;
        }
    }

    private TypeNode parseType(Header header, TypeContext enclosing, int depth) {
        if (depth > MAXIMUM_TYPE_NESTING) {
            throw new SyntaxFailure(ProblemKind.NESTING_TOO_DEEP,
                    "Types are nested deeper than " + MAXIMUM_TYPE_NESTING + " levels.", source.span(current()));
        }
        DeclarationKind kind;
        if (atKeyword("class")) {
            kind = DeclarationKind.CLASS;
        } else if (atKeyword("interface")) {
            kind = DeclarationKind.INTERFACE;
        } else if (atKeyword("enum")) {
            kind = DeclarationKind.ENUM;
        } else if (at(AT)) {
            kind = DeclarationKind.ANNOTATION_TYPE;
            advance();
        } else {
            kind = DeclarationKind.RECORD;
        }
        advance();

        String name          = expectIdentifier("a type name").value();
        String qualifiedName = enclosing == null ? qualify(packageName, name) : enclosing.qualifiedName + "." + name;

        List<TypeParameter> typeParameters   = at(LESS) ? parseTypeParameters() : List.of();
        ParsedComponents    components       = kind == DeclarationKind.RECORD ? parseRecordComponents(qualifiedName) : ParsedComponents.NONE;
        List<String>        extendedTypes    = List.of();
        List<String>        implementedTypes = List.of();
        List<String>        permittedTypes   = List.of();

        while (true) {
            if (atKeyword("extends")) {
                advance();
                extendedTypes = parseTypeList();
            } else if (atKeyword("implements")) {
                advance();
                implementedTypes = parseTypeList();
            } else if (isIdentifier(current(), "permits")) {
                advance();
                permittedTypes = parseTypeList();
            } else {
                break;
            }
        }
        if (!at(LEFT_BRACE)) {
            throw failure("Expected '{' to open the body of " + name + ".");
        }

        SourceSpan  signatureSpan = spanFrom(header.startIndex());
        String      signature     = collapse(signatureSpan);
        SymbolId    symbolId      = registerSymbol(qualifiedName);
        TypeContext context       = new TypeContext(kind, name, qualifiedName, components.identityTypes());
        Token       open          = advance();

        List<JavaNode> members = parseTypeBody(context, depth);
        int            end;
        if (at(RIGHT_BRACE)) {
            end = endOf(advance());
        } else {
            problems.add(new ParseProblem(ProblemKind.UNEXPECTED_END_OF_FILE, "The body of " + name + " is never closed.", source.span(open)));
            end = endOfPrevious();
        }

        SourceSpan bodySpan = lines.span(open.offset(), end);
        SourceSpan span     = lines.span(tokenAt(header.startIndex()).offset(), end);
        JavaDoc    javaDoc  = documentationFor(header.startIndex(), new JavaDocOwner(symbolId, kind, qualifiedName, signature, span));

        TypeDeclaration typeHeader = new TypeDeclaration(kind, name, qualifiedName, symbolId, header.modifiers(), header.annotations(),
                typeParameters, extendedTypes, implementedTypes, permittedTypes, List.of(), List.of(), signature, signatureSpan,
                bodySpan, span, javaDoc);
        TypeNode node = withTrivia(new TypeNode(typeHeader), header.startIndex());
        for (RecordComponentDeclaration component : components.components()) {
            node.add(new MemberNode(component));
        }
        members.forEach(node::add);
        return node;
    }

    private List<JavaNode> parseTypeBody(TypeContext context, int depth) {
        List<JavaNode> members = new ArrayList<>();
        if (context.kind == DeclarationKind.ENUM) {
            parseEnumConstants(context, members);
        }
        while (!at(RIGHT_BRACE) && !at(END_OF_FILE)) {
            TokenCursor.Savepoint start = cursor.savepoint();
            try {
                parseMember(context, members, depth);
            } catch (SyntaxFailure failure) {
                problems.add(failure.problem());
                recover(start, false);
            }
        }
        return members;
    }

    private List<TypeParameter> parseTypeParameters() {
        advance();
        List<TypeParameter> parameters = new ArrayList<>();
        while (true) {
            int              startIndex  = position();
            List<Annotation> annotations = parseAnnotations();
            String           name        = expectIdentifier("a type parameter").value();
            List<String>     bounds      = new ArrayList<>();
            if (atKeyword("extends")) {
                advance();
                bounds.add(parseTypeText());
                while (at(AMPERSAND)) {
                    advance();
                    bounds.add(parseTypeText());
                }
            }
            parameters.add(new TypeParameter(name, bounds, annotations, spanFrom(startIndex)));
            if (at(COMMA)) {
                advance();
                continue;
            }
            expect(GREATER, "Expected ',' or '>' in the type parameters.");
            return parameters;
        }
    }

    /** A record header's components and their identity types, which also identify the compact constructor. */
    private record ParsedComponents(List<RecordComponentDeclaration> components, List<String> identityTypes) {

        private static final ParsedComponents NONE = new ParsedComponents(List.of(), List.of());
    }

    private ParsedComponents parseRecordComponents(String qualifiedName) {
        expect(LEFT_PARENTHESIS, "Expected '(' to open the record header.");
        List<RecordComponentDeclaration> components    = new ArrayList<>();
        List<String>                     identityTypes = new ArrayList<>();
        if (at(RIGHT_PARENTHESIS)) {
            advance();
            return new ParsedComponents(components, identityTypes);
        }
        while (true) {
            int              startIndex     = position();
            List<Annotation> annotations    = parseAnnotations();
            int              typeStartIndex = position();
            String           type           = parseTypeText();
            int              typeEndIndex   = position();
            parseAnnotations();
            boolean variableArity = false;
            if (at(ELLIPSIS)) {
                advance();
                variableArity = true;
            }
            String     name = expectIdentifier("a record component name").value();
            SourceSpan span = spanFrom(startIndex);
            identityTypes.add(identityType(typeStartIndex, typeEndIndex) + (variableArity ? "..." : ""));
            components.add(new RecordComponentDeclaration(name, qualifiedName + "." + name, registerSymbol(qualifiedName + "#" + name),
                    annotations, type, variableArity, collapse(span), span));
            if (at(COMMA)) {
                advance();
                continue;
            }
            expect(RIGHT_PARENTHESIS, "Expected ',' or ')' in the record header.");
            return new ParsedComponents(components, identityTypes);
        }
    }

    // ------------------------------------------------------------------ members

    private void parseMember(TypeContext context, List<JavaNode> members, int depth) {
        if (at(SEMICOLON)) {
            advance();
            return;
        }
        if (at(LEFT_BRACE)) {
            members.add(parseInitializer(context, false));
            return;
        }
        if (atKeyword("static") && LEFT_BRACE.matches(peek(1))) {
            members.add(parseInitializer(context, true));
            return;
        }

        Header header = parseHeader();
        if (isTypeStart()) {
            members.add(parseType(header, context, depth + 1));
            return;
        }
        List<TypeParameter> typeParameters = at(LESS) ? parseTypeParameters() : List.of();
        if (at(IDENTIFIER) && LEFT_PARENTHESIS.matches(peek(1))) {
            members.add(parseConstructor(header, typeParameters, context));
            return;
        }
        if (context.kind == DeclarationKind.RECORD && typeParameters.isEmpty() && isIdentifier(current(), context.name)
                && LEFT_BRACE.matches(peek(1))) {
            members.add(parseCompactConstructor(header, context));
            return;
        }

        int    typeStartIndex = position();
        String type           = parseTypeText();
        int    typeEndIndex   = position();
        Token  nameToken      = expectIdentifier("a member name");
        if (at(LEFT_PARENTHESIS)) {
            members.add(parseMethod(header, typeParameters, type, nameToken, context));
            return;
        }
        if (!typeParameters.isEmpty()) {
            throw failure("Type parameters must be followed by a method or constructor.");
        }
        parseFields(header, typeStartIndex, typeEndIndex, type, nameToken, context, members);
    }

    private MemberNode parseInitializer(TypeContext context, boolean isStatic) {
        int             startIndex    = position();
        Token           staticToken   = isStatic ? advance() : null;
        SourceSpan      body          = skipBody();
        int             ordinal       = context.nextInitializerOrdinal(isStatic);
        String          name          = isStatic ? "<static-initializer>" : "<initializer>";
        String          qualified     = context.qualifiedName + "." + name;
        SymbolId        symbolId      = registerSymbol(context.qualifiedName + "#" + name + "[" + ordinal + "]");
        SourceSpan      signatureSpan = isStatic ? source.span(staticToken) : lines.span(body.start().offset(), body.start().offset());
        String          signature     = isStatic ? "static" : "";
        SourceSpan      span          = spanFrom(startIndex);
        DeclarationKind kind          = isStatic ? DeclarationKind.STATIC_INITIALIZER : DeclarationKind.INITIALIZER;
        JavaDoc         javaDoc       = documentationFor(startIndex, new JavaDocOwner(symbolId, kind, qualified, signature, span));
        return withTrivia(new MemberNode(new InitializerDeclaration(isStatic, ordinal, qualified, symbolId, signature, signatureSpan,
                body, span, javaDoc)), startIndex);
    }

    private MemberNode parseMethod(Header header, List<TypeParameter> typeParameters, String returnType, Token nameToken,
                                   TypeContext context) {
        ParsedParameters parameters         = parseParameters();
        String           declaredReturnType = returnType;
        while (at(LEFT_BRACKET) && RIGHT_BRACKET.matches(peek(1))) {
            cursor.shift(2);
            declaredReturnType = declaredReturnType + "[]";
        }
        List<String> thrownTypes   = parseThrows();
        SourceSpan   signatureSpan = spanFrom(header.startIndex());
        String       defaultValue  = null;
        if (atKeyword("default")) {
            advance();
            int valueStart = current().offset();
            skipExpression();
            defaultValue = source.subSequence(valueStart, endOfPrevious()).toString();
        }
        SourceSpan body = null;
        if (at(LEFT_BRACE)) {
            body = skipBody();
        } else {
            expect(SEMICOLON, "Expected a method body or ';'.");
        }

        String     name          = nameToken.value();
        String     qualifiedName = context.qualifiedName + "." + name;
        SymbolId   symbolId      = registerSymbol(context.qualifiedName + "#" + name + "(" + String.join(",", parameters.identityTypes()) + ")");
        SourceSpan span          = spanFrom(header.startIndex());
        String     signature     = collapse(signatureSpan);
        JavaDoc    javaDoc       = documentationFor(header.startIndex(),
                new JavaDocOwner(symbolId, DeclarationKind.METHOD, qualifiedName, signature, span));

        return withTrivia(new MemberNode(new MethodDeclaration(name, qualifiedName, symbolId, header.modifiers(), header.annotations(),
                typeParameters, declaredReturnType, parameters.parameters(), thrownTypes, defaultValue, signature, signatureSpan, body,
                span, javaDoc)), header.startIndex());
    }

    private MemberNode parseConstructor(Header header, List<TypeParameter> typeParameters, TypeContext context) {
        Token nameToken = advance();
        if (!nameToken.value().equals(context.name)) {
            problems.add(new ParseProblem(ProblemKind.UNEXPECTED_TOKEN,
                    "A method needs a return type; '" + nameToken.value() + "' is not the name of " + context.name + ".", source.span(nameToken)));
        }
        ParsedParameters parameters    = parseParameters();
        List<String>     thrownTypes   = parseThrows();
        SourceSpan       signatureSpan = spanFrom(header.startIndex());
        if (!at(LEFT_BRACE)) {
            throw failure("Expected a constructor body.");
        }
        SourceSpan body     = skipBody();
        String     identity = context.qualifiedName + "#" + nameToken.value() + "(" + String.join(",", parameters.identityTypes()) + ")";
        return constructor(header, typeParameters, context, nameToken.value(), parameters.parameters(), thrownTypes, false,
                identity, signatureSpan, body);
    }

    private MemberNode parseCompactConstructor(Header header, TypeContext context) {
        Token      nameToken     = advance();
        SourceSpan signatureSpan = spanFrom(header.startIndex());
        SourceSpan body          = skipBody();
        String     identity      = context.qualifiedName + "#" + nameToken.value() + "(" + String.join(",", context.componentIdentityTypes) + ")";
        return constructor(header, List.of(), context, nameToken.value(), List.of(), List.of(), true, identity, signatureSpan, body);
    }

    private MemberNode constructor(Header header, List<TypeParameter> typeParameters, TypeContext context, String name,
                                   List<Parameter> parameters, List<String> thrownTypes, boolean compact,
                                   String identity, SourceSpan signatureSpan, SourceSpan body) {
        String          qualifiedName = context.qualifiedName + "." + name;
        SymbolId        symbolId      = registerSymbol(identity);
        SourceSpan      span          = spanFrom(header.startIndex());
        String          signature     = collapse(signatureSpan);
        DeclarationKind kind          = compact ? DeclarationKind.COMPACT_CONSTRUCTOR : DeclarationKind.CONSTRUCTOR;
        JavaDoc         javaDoc       = documentationFor(header.startIndex(), new JavaDocOwner(symbolId, kind, qualifiedName, signature, span));
        return withTrivia(new MemberNode(new ConstructorDeclaration(name, qualifiedName, symbolId, header.modifiers(), header.annotations(),
                typeParameters, parameters, thrownTypes, compact, signature, signatureSpan, body, span, javaDoc)), header.startIndex());
    }

    /** One variable of a field statement, before the statement's end is known. */
    private record FieldVariable(String name, String type, SourceSpan span) {
    }

    private void parseFields(Header header, int typeStartIndex, int typeEndIndex, String type, Token firstName, TypeContext context,
                             List<JavaNode> members) {
        SourceSpan          signatureSpan   = lines.span(tokenAt(header.startIndex()).offset(), endOf(tokenAt(typeEndIndex - 1)));
        String              headerSignature = collapse(signatureSpan);
        List<FieldVariable> variables       = new ArrayList<>();
        Token               nameToken       = firstName;

        while (true) {
            String variableType = type;
            while (at(LEFT_BRACKET) && RIGHT_BRACKET.matches(peek(1))) {
                cursor.shift(2);
                variableType = variableType + "[]";
            }
            if (at(ASSIGN)) {
                advance();
                skipExpression();
            }
            variables.add(new FieldVariable(nameToken.value(), variableType, lines.span(nameToken.offset(), endOfPrevious())));
            if (at(COMMA)) {
                advance();
                nameToken = expectIdentifier("a field name");
                continue;
            }
            expect(SEMICOLON, "Expected ';' after the field declaration.");
            break;
        }

        SourceSpan   span    = spanFrom(header.startIndex());
        List<Trivia> trivia  = leadingTrivia(header.startIndex());
        for (FieldVariable variable : variables) {
            String   qualifiedName = context.qualifiedName + "." + variable.name();
            SymbolId symbolId      = registerSymbol(context.qualifiedName + "#" + variable.name());
            String   signature     = headerSignature + " " + variable.name();
            JavaDoc  javaDoc       = documentationFor(header.startIndex(),
                    new JavaDocOwner(symbolId, DeclarationKind.FIELD, qualifiedName, signature, span));
            MemberNode node = new MemberNode(new FieldDeclaration(variable.name(), qualifiedName, symbolId, header.modifiers(),
                    header.annotations(), variable.type(), signature, signatureSpan, variable.span(), span, javaDoc));
            node.addLeadingTrivia(trivia);
            members.add(node);
        }
    }

    private void parseEnumConstants(TypeContext context, List<JavaNode> members) {
        while (true) {
            if (at(SEMICOLON)) {
                advance();
                return;
            }
            if (at(RIGHT_BRACE) || at(END_OF_FILE)) {
                return;
            }
            TokenCursor.Savepoint start      = cursor.savepoint();
            int                   startIndex = start.getPosition();
            try {
                List<Annotation> annotations = parseAnnotations();
                Token            nameToken   = expectIdentifier("an enum constant");
                String           arguments   = null;
                if (at(LEFT_PARENTHESIS)) {
                    Token open  = current();
                    Token close = skipBalanced(LEFT_PARENTHESIS, RIGHT_PARENTHESIS);
                    arguments = source.subSequence(endOf(open), close == null ? current().offset() : close.offset()).toString();
                }
                SourceSpan signatureSpan = spanFrom(startIndex);
                SourceSpan body          = at(LEFT_BRACE) ? skipBody() : null;
                SourceSpan span          = spanFrom(startIndex);
                String     name          = nameToken.value();
                String     qualifiedName = context.qualifiedName + "." + name;
                SymbolId   symbolId      = registerSymbol(context.qualifiedName + "#" + name);
                String     signature     = collapse(signatureSpan);
                JavaDoc    javaDoc       = documentationFor(startIndex,
                        new JavaDocOwner(symbolId, DeclarationKind.ENUM_CONSTANT, qualifiedName, signature, span));
                members.add(withTrivia(new MemberNode(new EnumConstantDeclaration(name, qualifiedName, symbolId, annotations, arguments,
                        signature, signatureSpan, body, span, javaDoc)), startIndex));
                if (at(COMMA)) {
                    advance();
                    continue;
                }
                if (at(SEMICOLON)) {
                    advance();
                    return;
                }
                if (at(RIGHT_BRACE)) {
                    return;
                }
                throw failure("Expected ',', ';' or '}' after an enum constant.");
            } catch (SyntaxFailure failure) {
                problems.add(failure.problem());
                if (recoverEnumConstant(start)) {
                    return;
                }
            }
        }
    }

    // ------------------------------------------------------------------ parameters and throws

    /** Formal parameters and the types that identify them in a symbol id; a receiver parameter has none. */
    private record ParsedParameters(List<Parameter> parameters, List<String> identityTypes) {
    }

    private ParsedParameters parseParameters() {
        expect(LEFT_PARENTHESIS, "Expected '('.");
        List<Parameter> parameters    = new ArrayList<>();
        List<String>    identityTypes = new ArrayList<>();
        if (at(RIGHT_PARENTHESIS)) {
            advance();
            return new ParsedParameters(parameters, identityTypes);
        }
        while (true) {
            int              startIndex     = position();
            Header           header         = parseHeader();
            int              typeStartIndex = position();
            String           type           = parseTypeText();
            int              typeEndIndex   = position();
            List<Annotation> annotations    = new ArrayList<>(header.annotations());
            annotations.addAll(parseAnnotations());
            boolean variableArity = false;
            if (at(ELLIPSIS)) {
                advance();
                variableArity = true;
            }
            String  name;
            boolean receiver = false;
            if (atKeyword("this")) {
                name = advance().value();
                receiver = true;
            } else if (at(IDENTIFIER) && DOT.matches(peek(1)) && isKeyword(peek(2), "this")) {
                name = advance().value() + advance().value() + advance().value();
                receiver = true;
            } else {
                name = expectIdentifier("a parameter name").value();
            }
            String dimensions = "";
            while (at(LEFT_BRACKET) && RIGHT_BRACKET.matches(peek(1))) {
                cursor.shift(2);
                dimensions = dimensions + "[]";
            }
            parameters.add(new Parameter(name, type + dimensions, annotations, header.modifiers(), variableArity, receiver, spanFrom(startIndex)));
            if (!receiver) {
                identityTypes.add(identityType(typeStartIndex, typeEndIndex) + dimensions + (variableArity ? "..." : ""));
            }
            if (at(COMMA)) {
                advance();
                continue;
            }
            expect(RIGHT_PARENTHESIS, "Expected ',' or ')' in the parameter list.");
            return new ParsedParameters(parameters, identityTypes);
        }
    }

    private List<String> parseThrows() {
        if (!atKeyword("throws")) {
            return List.of();
        }
        advance();
        return parseTypeList();
    }

    // ------------------------------------------------------------------ type references

    private List<String> parseTypeList() {
        List<String> types = new ArrayList<>();
        types.add(parseTypeText());
        while (at(COMMA)) {
            advance();
            types.add(parseTypeText());
        }
        return types;
    }

    private String parseTypeText() {
        int start = position();
        int end   = scanType(start);
        if (end < 0) {
            throw failure("Expected a type.");
        }
        moveTo(end);
        return collapse(lines.span(tokenAt(start).offset(), endOf(tokenAt(end - 1))));
    }

    /** Where a type starting at absolute {@code position} ends, or {@code -1}; never moves the cursor. */
    private int scanType(int position) {
        int scan = skipAnnotationsAt(position);
        if (scan < 0) {
            return -1;
        }
        Token token = tokenAt(scan);
        if (KEYWORD.matches(token) && PRIMITIVE_TYPES.contains(token.value())) {
            scan++;
        } else if (IDENTIFIER.matches(token)) {
            scan++;
            if (LESS.matches(tokenAt(scan))) {
                scan = scanTypeArguments(scan);
                if (scan < 0) {
                    return -1;
                }
            }
            while (DOT.matches(tokenAt(scan)) && (IDENTIFIER.matches(tokenAt(scan + 1)) || AT.matches(tokenAt(scan + 1)))) {
                scan = skipAnnotationsAt(scan + 1);
                if (scan < 0 || !IDENTIFIER.matches(tokenAt(scan))) {
                    return -1;
                }
                scan++;
                if (LESS.matches(tokenAt(scan))) {
                    scan = scanTypeArguments(scan);
                    if (scan < 0) {
                        return -1;
                    }
                }
            }
        } else {
            return -1;
        }
        while (true) {
            int candidate = skipAnnotationsAt(scan);
            if (candidate >= 0 && LEFT_BRACKET.matches(tokenAt(candidate)) && RIGHT_BRACKET.matches(tokenAt(candidate + 1))) {
                scan = candidate + 2;
            } else {
                return scan;
            }
        }
    }

    /** Where type arguments starting at the {@code <} at absolute {@code position} end, or {@code -1}. */
    private int scanTypeArguments(int position) {
        int scan = position + 1;
        if (GREATER.matches(tokenAt(scan))) {
            return scan + 1;
        }
        while (true) {
            scan = skipAnnotationsAt(scan);
            if (scan < 0) {
                return -1;
            }
            if (QUESTION.matches(tokenAt(scan))) {
                scan++;
                if (isKeyword(tokenAt(scan), "extends") || isKeyword(tokenAt(scan), "super")) {
                    scan = scanType(scan + 1);
                }
            } else {
                scan = scanType(scan);
            }
            if (scan < 0) {
                return -1;
            }
            if (COMMA.matches(tokenAt(scan))) {
                scan++;
                continue;
            }
            if (GREATER.matches(tokenAt(scan))) {
                return scan + 1;
            }
            return -1;
        }
    }

    /** Where the annotations starting at absolute {@code position} end ({@code position} itself when none), or {@code -1}. */
    private int skipAnnotationsAt(int position) {
        int scan = position;
        while (AT.matches(tokenAt(scan)) && !isKeyword(tokenAt(scan + 1), "interface")) {
            scan++;
            if (!IDENTIFIER.matches(tokenAt(scan))) {
                return -1;
            }
            scan++;
            while (DOT.matches(tokenAt(scan)) && IDENTIFIER.matches(tokenAt(scan + 1))) {
                scan += 2;
            }
            if (LEFT_PARENTHESIS.matches(tokenAt(scan))) {
                int depth = 0;
                do {
                    Token token = tokenAt(scan);
                    if (END_OF_FILE.matches(token)) {
                        return -1;
                    }
                    if (LEFT_PARENTHESIS.matches(token)) {
                        depth++;
                    } else if (RIGHT_PARENTHESIS.matches(token)) {
                        depth--;
                    }
                    scan++;
                } while (depth > 0);
            }
        }
        return scan;
    }

    /**
     * The type as a symbol id writes it: annotations, type arguments and whitespace dropped, every other
     * token kept as written — {@code java.util.@NonNull List<String>[]} becomes {@code java.util.List[]}.
     */
    private String identityType(int fromIndex, int toIndex) {
        StringBuilder text = new StringBuilder();
        int           scan = fromIndex;
        while (scan < toIndex) {
            Token token = tokenAt(scan);
            if (AT.matches(token) && !isKeyword(tokenAt(scan + 1), "interface")) {
                int end = skipAnnotationsAt(scan);
                scan = end < 0 ? toIndex : end;
                continue;
            }
            if (LESS.matches(token)) {
                int depth = 0;
                do {
                    if (LESS.matches(tokenAt(scan))) {
                        depth++;
                    } else if (GREATER.matches(tokenAt(scan))) {
                        depth--;
                    }
                    scan++;
                } while (depth > 0 && scan < toIndex);
                continue;
            }
            text.append(token.value());
            scan++;
        }
        return text.toString();
    }

    // ------------------------------------------------------------------ opaque regions

    /** Consumes a brace-delimited body and records it as opaque. Precondition: at an opening brace. */
    private SourceSpan skipBody() {
        Token      open  = current();
        Token      close = skipBalanced(LEFT_BRACE, RIGHT_BRACE);
        int        end   = close == null ? endOfPrevious() : endOf(close);
        SourceSpan body  = lines.span(open.offset(), end);
        opaqueBodies.add(body);
        return body;
    }

    /**
     * Consumes from the opening token at the cursor through its matching closing token, counting only that
     * pair — literals and comments are already single tokens, so they cannot unbalance it.
     *
     * @return the closing token, or {@code null} when the text ends first (reported)
     */
    private Token skipBalanced(JavaTokenKind opening, JavaTokenKind closing) {
        Token open  = advance();
        int   depth = 1;
        while (true) {
            if (at(END_OF_FILE)) {
                problems.add(new ParseProblem(ProblemKind.UNBALANCED_DELIMITER, "'" + open.value() + "' is never closed.", source.span(open)));
                return null;
            }
            Token token = advance();
            if (opening.matches(token)) {
                depth++;
            } else if (closing.matches(token)) {
                depth--;
                if (depth == 0) {
                    return token;
                }
            }
        }
    }

    /**
     * Consumes an initializer or default value: everything up to a {@code ,}, {@code ;} or closing
     * bracket that is not nested in brackets or type arguments.
     */
    private void skipExpression() {
        int depth = 0;
        while (!at(END_OF_FILE)) {
            Token token = current();
            if (depth == 0 && (COMMA.matches(token) || SEMICOLON.matches(token) || RIGHT_BRACE.matches(token)
                    || RIGHT_PARENTHESIS.matches(token) || RIGHT_BRACKET.matches(token))) {
                return;
            }
            if (LESS.matches(token)) {
                int end = scanTypeArguments(position());
                if (end > 0) {
                    moveTo(end);
                    continue;
                }
            }
            if (LEFT_PARENTHESIS.matches(token) || LEFT_BRACKET.matches(token) || LEFT_BRACE.matches(token)) {
                depth++;
            } else if (RIGHT_PARENTHESIS.matches(token) || RIGHT_BRACKET.matches(token) || RIGHT_BRACE.matches(token)) {
                depth--;
            }
            advance();
        }
    }

    // ------------------------------------------------------------------ recovery

    /**
     * Skips a declaration that failed to parse: restores the cursor to its first token and stops after
     * its semicolon or after the closing brace of the first brace block it opens. A closing brace that
     * would close the enclosing type body is left for the caller; at the top level it is consumed as
     * stray. Always makes progress.
     */
    private void recover(TokenCursor.Savepoint start, boolean topLevel) {
        cursor.restore(start);
        int depth = 0;
        while (!at(END_OF_FILE)) {
            Token token = current();
            if (LEFT_PARENTHESIS.matches(token) || LEFT_BRACKET.matches(token) || LEFT_BRACE.matches(token)) {
                depth++;
            } else if (RIGHT_PARENTHESIS.matches(token) || RIGHT_BRACKET.matches(token) || RIGHT_BRACE.matches(token)) {
                if (depth == 0) {
                    if (topLevel || !RIGHT_BRACE.matches(token)) {
                        advance();
                    }
                    return;
                }
                depth--;
                if (depth == 0 && RIGHT_BRACE.matches(token)) {
                    advance();
                    return;
                }
            } else if (SEMICOLON.matches(token) && depth == 0) {
                advance();
                return;
            }
            advance();
        }
    }

    /**
     * Skips an enum constant that failed to parse, through the next separating comma.
     *
     * @return {@code true} when the constant list ended — a {@code ;} was consumed or a closing brace is next
     */
    private boolean recoverEnumConstant(TokenCursor.Savepoint start) {
        cursor.restore(start);
        int depth = 0;
        while (!at(END_OF_FILE)) {
            Token token = current();
            if (depth == 0) {
                if (COMMA.matches(token)) {
                    advance();
                    return false;
                }
                if (SEMICOLON.matches(token)) {
                    advance();
                    return true;
                }
                if (RIGHT_BRACE.matches(token)) {
                    return true;
                }
                if (RIGHT_PARENTHESIS.matches(token) || RIGHT_BRACKET.matches(token)) {
                    advance();
                    return false;
                }
            }
            if (LEFT_PARENTHESIS.matches(token) || LEFT_BRACKET.matches(token) || LEFT_BRACE.matches(token)) {
                depth++;
            } else if (RIGHT_PARENTHESIS.matches(token) || RIGHT_BRACKET.matches(token) || RIGHT_BRACE.matches(token)) {
                depth--;
            }
            advance();
        }
        return true;
    }

    // ------------------------------------------------------------------ comments: trivia and documentation

    /** Attaches the comments written before a declaration as its leading jME trivia. */
    private <N extends JavaNode> N withTrivia(N node, int startIndex) {
        node.addLeadingTrivia(leadingTrivia(startIndex));
        return node;
    }

    /** Every comment between the previous token and the declaration's first token, as written. */
    private List<Trivia> leadingTrivia(int startIndex) {
        int          gapStart = gapStart(startIndex);
        int          gapEnd   = tokenAt(startIndex).offset();
        List<Trivia> trivia   = new ArrayList<>();
        for (int position = firstCommentAtOrAfter(gapStart); position < comments.size(); position++) {
            JavaComment comment = comments.get(position);
            if (comment.span().end().offset() > gapEnd) {
                break;
            }
            trivia.add(Trivia.comment(comment.text()));
        }
        return trivia;
    }

    private int firstCommentAtOrAfter(int offset) {
        int low  = 0;
        int high = comments.size();
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (comments.get(middle).span().start().offset() < offset) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        return low;
    }

    /**
     * The documentation comment of the declaration whose first token is at {@code startIndex}: the last
     * documentation comment between the previous token and that one. Each call returns a copy owned by
     * the given owner and records the pairing.
     */
    private JavaDoc documentationFor(int startIndex, JavaDocOwner owner) {
        int gapStart = gapStart(startIndex);
        int gapEnd   = tokenAt(startIndex).offset();
        int found    = -1;
        for (int position = lastDocumentationStartingBefore(gapEnd); position >= 0; position--) {
            JavaComment comment = documentationComments.get(position);
            if (comment.span().start().offset() < gapStart) {
                break;
            }
            if (comment.span().end().offset() <= gapEnd) {
                found = position;
                break;
            }
        }
        if (found < 0) {
            return null;
        }
        claimedDocumentation[found] = true;
        JavaDoc owned = parsedDocumentation(found).withOwner(owner);
        javaDocs.add(owned);
        return owned;
    }

    private int gapStart(int startIndex) {
        return startIndex == 0 ? 0 : endOf(tokenAt(startIndex - 1));
    }

    private int lastDocumentationStartingBefore(int offset) {
        int low    = 0;
        int high   = documentationComments.size() - 1;
        int result = -1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            if (documentationComments.get(middle).span().start().offset() < offset) {
                result = middle;
                low = middle + 1;
            } else {
                high = middle - 1;
            }
        }
        return result;
    }

    private JavaDoc parsedDocumentation(int position) {
        if (parsedDocumentation[position] == null) {
            JavaComment comment = documentationComments.get(position);
            parsedDocumentation[position] = javaDocParser.parse(comment.text(), comment.span().start().offset(), lines, null);
        }
        return parsedDocumentation[position];
    }

    private void addOrphanDocumentation() {
        for (int position = 0; position < documentationComments.size(); position++) {
            if (claimedDocumentation[position]) {
                continue;
            }
            SourceSpan span       = documentationComments.get(position).span();
            boolean    insideBody = opaqueBodies.stream().anyMatch(body -> body.contains(span));
            if (!insideBody) {
                javaDocs.add(parsedDocumentation(position));
            }
        }
    }

    // ------------------------------------------------------------------ symbols and text

    private SymbolId registerSymbol(String identity) {
        int count = symbolCounts.merge(identity, 1, Integer::sum);
        return new SymbolId(count == 1 ? identity : identity + "~" + count);
    }

    private static String qualify(String packageName, String name) {
        return packageName.isEmpty() ? name : packageName + "." + name;
    }

    /** The covered text with each run of whitespace reduced to one space and the ends trimmed. */
    private String collapse(SourceSpan span) {
        StringBuilder text         = new StringBuilder(span.length());
        boolean       pendingSpace = false;
        for (int offset = span.start().offset(); offset < span.end().offset(); offset++) {
            char character = source.charAt(offset);
            if (Character.isWhitespace(character)) {
                pendingSpace = !text.isEmpty();
                continue;
            }
            if (pendingSpace) {
                text.append(' ');
                pendingSpace = false;
            }
            text.append(character);
        }
        return text.toString();
    }

    // ------------------------------------------------------------------ cursor access

    private Token current() {
        return cursor.peek();
    }

    private Token peek(int distance) {
        return cursor.lookAt(distance);
    }

    /** The token at an absolute position; the cursor clamps it to the first and the end-of-file token. */
    private Token tokenAt(int position) {
        return cursor.lookAt(position - cursor.position());
    }

    private int position() {
        return cursor.position();
    }

    private void moveTo(int position) {
        cursor.restore(() -> position);
    }

    private Token advance() {
        Token token = current();
        if (!END_OF_FILE.matches(token)) {
            cursor.next();
        }
        return token;
    }

    private boolean at(JavaTokenKind kind) {
        return kind.matches(current());
    }

    private boolean atKeyword(String keyword) {
        return isKeyword(current(), keyword);
    }

    private Token expect(JavaTokenKind kind, String message) {
        if (!at(kind)) {
            throw failure(message);
        }
        return advance();
    }

    private Token expectIdentifier(String expectation) {
        if (!at(IDENTIFIER)) {
            throw failure("Expected " + expectation + ".");
        }
        return advance();
    }

    private SyntaxFailure failure(String message) {
        Token       token = current();
        boolean     end   = END_OF_FILE.matches(token);
        ProblemKind kind  = end ? ProblemKind.UNEXPECTED_END_OF_FILE : ProblemKind.UNEXPECTED_TOKEN;
        String      found = end ? "the end of the file" : "'" + token.value() + "'";
        return new SyntaxFailure(kind, message + " Found " + found + ".", source.span(token));
    }

    private int endOf(Token token) {
        return source.endOffset(token);
    }

    /** From the first token of a construct to the end of the last consumed token. */
    private SourceSpan spanFrom(int startIndex) {
        int start = tokenAt(startIndex).offset();
        return lines.span(start, Math.max(start, endOfPrevious()));
    }

    private int endOfPrevious() {
        if (cursor.position() == 0) {
            return 0;
        }
        return endOf(peek(-1));
    }
}
