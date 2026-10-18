package org.jmouse.java.parser;

import org.jmouse.el.lexer.Token;
import org.jmouse.el.lexer.TokenCursor;
import org.jmouse.java.parser.lexer.JavaLexer;
import org.jmouse.java.parser.lexer.JavaSourceText;
import org.jmouse.java.parser.lexer.JavaTokenKind;
import org.jmouse.java.parser.model.BodyReference;
import org.jmouse.java.parser.model.ConstructorDeclaration;
import org.jmouse.java.parser.model.Declaration;
import org.jmouse.java.parser.model.ExecutableBody;
import org.jmouse.java.parser.model.Expression;
import org.jmouse.java.parser.model.FieldDeclaration;
import org.jmouse.java.parser.model.InitializerDeclaration;
import org.jmouse.java.parser.model.JavaSourceFile;
import org.jmouse.java.parser.model.LocalBinding;
import org.jmouse.java.parser.model.MethodDeclaration;
import org.jmouse.java.parser.model.Parameter;
import org.jmouse.java.parser.model.SymbolId;
import org.jmouse.java.parser.model.TypeDeclaration;
import org.jmouse.java.parser.source.ParseProblem;
import org.jmouse.java.parser.source.ProblemKind;
import org.jmouse.java.parser.source.SourceSpan;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Recovery-oriented body reader. Reuses Java tokens and positions; never resolves names. */
final class BodyParser {
    private static final Set<String> PRIMITIVES =
            Set.of("boolean", "byte", "short", "char", "int", "long", "float", "double", "var");
    private static final Set<String> CONTROL =
            Set.of("if", "while", "switch", "synchronized", "catch", "for", "try");
    private static final Set<String> ASSIGNMENTS =
            Set.of("=", "+=", "-=", "*=", "/=", "%=", "&=", "|=", "^=", "<<=", ">>=", ">>>=");
    private final JavaSourceText source;
    private final List<Token> tokens = new ArrayList<>();
    private final List<ParseProblem> problems;
    private final List<Expression> expressions = new ArrayList<>();
    private final List<LocalBinding> variables = new ArrayList<>();
    private int[] closing;
    private int position;
    private int limit;
    private int depth;

    private BodyParser(String name, CharSequence text, List<ParseProblem> problems) {
        source = new JavaSourceText(name, text);
        TokenCursor cursor = new JavaLexer().tokenize(source);
        while (!JavaTokenKind.END_OF_FILE.matches(cursor.peek())) {
            tokens.add(cursor.next());
        }
        tokens.add(cursor.peek());
        this.problems = problems;
        closing = new int[tokens.size()];
        Arrays.fill(closing, -1);
        ArrayDeque<Integer> openings = new ArrayDeque<>();
        for (int index = 0; index < tokens.size(); index++) {
            String value = word(index);
            if (Set.of("(", "[", "{").contains(value)) {
                openings.push(index);
            } else if (Set.of(")", "]", "}").contains(value)) {
                while (!openings.isEmpty() && !matches(word(openings.peek()), value)) {
                    openings.pop();
                }
                if (!openings.isEmpty()) {
                    closing[openings.pop()] = index;
                }
            }
        }
    }

    static JavaSourceFile enrich(JavaSourceFile file, CharSequence text) {
        List<ParseProblem> problems = new ArrayList<>(file.problems());
        BodyParser parser = new BodyParser(file.sourceName(), text, problems);
        List<ExecutableBody> bodies = new ArrayList<>();
        for (TypeDeclaration type : file.types()) {
            parser.type(type, bodies);
        }
        problems.sort(Comparator.comparingInt(problem -> problem.span().start().offset()));
        return new JavaSourceFile(
                file.sourceName(),
                file.packageDeclaration(),
                file.moduleDeclaration(),
                file.imports(),
                file.types(),
                file.comments(),
                file.javaDocs(),
                problems,
                file.typeReferences(),
                bodies);
    }

    private void type(TypeDeclaration type, List<ExecutableBody> bodies) {
        for (Declaration member : type.members()) {
            if (member instanceof TypeDeclaration nested) {
                type(nested, bodies);
                continue;
            }
            SourceSpan body =
                    switch (member) {
                        case MethodDeclaration method -> method.body();
                        case ConstructorDeclaration constructor -> constructor.body();
                        case InitializerDeclaration initializer -> initializer.body();
                        case FieldDeclaration field -> field.variableSpan();
                        default -> null;
                    };
            if (body == null) {
                continue;
            }
            position = tokenAt(body.start().offset());
            limit = tokenAt(body.end().offset());
            expressions.clear();
            variables.clear();
            boolean fieldInitializer = member instanceof FieldDeclaration;
            if (fieldInitializer) {
                while (position < limit && !at("=")) {
                    position++;
                }
                if (!take("=")) {
                    continue;
                }
            }
            List<Parameter> parameters =
                    switch (member) {
                        case MethodDeclaration method -> method.parameters();
                        case ConstructorDeclaration constructor -> constructor.parameters();
                        default -> List.of();
                    };
            for (Parameter parameter : parameters) {
                SourceSpan nameSpan = parameter.span();
                for (int index = tokenAt(parameter.span().start().offset());
                        index < tokens.size()
                                && tokens.get(index).offset() < parameter.span().end().offset();
                        index++) {
                    if (tokens.get(index).value().equals(parameter.name())) {
                        nameSpan = source.span(tokens.get(index));
                    }
                }
                variables.add(
                        new LocalBinding(
                                parameter.name(),
                                parameter.type() + (parameter.variableArity() ? "[]" : ""),
                                nameSpan,
                                body,
                                null));
            }
            if (fieldInitializer) {
                Expression initializer = expression(0);
                if (initializer == null) {
                    problem("Expected a field initializer.");
                }
                add(initializer);
            } else {
                block(body);
            }
            List<BodyReference> references = new ArrayList<>();
            for (Expression expression : expressions) {
                references(
                        expression,
                        BodyReference.Role.READS,
                        member.symbolId(),
                        type.symbolId(),
                        references);
            }
            references.sort(
                    Comparator.comparingInt(reference -> reference.span().start().offset()));
            bodies.add(
                    new ExecutableBody(
                            member.symbolId(),
                            type.symbolId(),
                            body,
                            expressions,
                            variables,
                            references));
        }
    }

    private void block(SourceSpan scope) {
        boolean braced = take("{");
        while (position < limit && !at("}")) {
            int before = position;
            statement(scope);
            if (before == position) {
                problem("Expected a statement or expression.");
                position++;
            }
        }
        if (braced && !take("}")) {
            problem("Unclosed executable block.");
        }
    }

    private void statement(SourceSpan scope) {
        if (++depth > 128) {
            problem("Executable nesting exceeds 128 levels.");
            position = Math.min(position + 1, limit);
            depth--;
            return;
        }
        try {
            if (at("{")) {
                block(scopeOf(position, scope));
            } else if (take(";")) {
                // Empty statement.
            } else if (CONTROL.contains(word(position))) {
                String control = word(position++);
                SourceSpan controlScope = controlScope(scope);
                if (take("(")) {
                    while (position < limit && !at(")") && !at("}")) {
                        int before = position;
                        if (!local(controlScope)) {
                            add(expression(0));
                        }
                        if (!(take(";") || take(":") || take(",")) && position == before) {
                            position++;
                        }
                    }
                    require(")");
                }
                if (!control.equals("try") || at("{")) {
                    statement(controlScope);
                }
            } else if (Set.of("else", "finally", "do").contains(word(position))) {
                position++;
                statement(scope);
            } else if (Set.of("return", "throw", "yield", "assert").contains(word(position))) {
                position++;
                add(expression(0));
                if (take(":")) {
                    add(expression(0));
                }
                require(";");
            } else if (Set.of("break", "continue").contains(word(position))) {
                while (position < limit && !at(";") && !at("}")) {
                    position++;
                }
                take(";");
            } else if (Set.of("class", "interface", "enum", "record").contains(word(position))) {
                unsupported("Local type declarations are not yet indexed.");
                while (position < limit && !at("{") && !at(";") && !at("}")) {
                    position++;
                }
                if (at("{") && closing[position] >= 0) {
                    position = closing[position] + 1;
                }
            } else if (at("case") || at("default")) {
                while (position < limit && !at(":") && !at("-") && !at("}")) {
                    position++;
                }
                if (take("-")) {
                    take(">");
                } else {
                    take(":");
                }
            } else if (local(scope)) {
                require(";");
            } else {
                add(expression(0));
                require(";");
            }
        } finally {
            depth--;
        }
    }

    private boolean local(SourceSpan scope) {
        int start = position;
        take("final");
        int typeStart = position;
        int typeEnd = typeEnd(position);
        if (typeEnd < 0 || !identifier(typeEnd)) {
            position = start;
            return false;
        }
        String writtenType = text(typeStart, typeEnd).replaceAll("\\s+", "");
        position = typeEnd;
        do {
            Token name = tokens.get(position++);
            String variableType = writtenType;
            while (take("[")) {
                require("]");
                variableType += "[]";
            }
            boolean initialized = take("=");
            Expression initializer = initialized ? expression(0) : null;
            if (initialized && initializer == null) {
                problem("Expected a variable initializer.");
            }
            SourceSpan visibility =
                    source.lines().span(source.endOffset(name), scope.end().offset());
            variables.add(
                    new LocalBinding(
                            name.value(),
                            variableType.equals("var") ? null : variableType,
                            source.span(name),
                            visibility,
                            initializer));
            add(initializer);
        } while (take(",") && identifier(position));
        return true;
    }

    /**
     * A conservative type scan: unlike an expression, a type must be followed by a declarator name.
     */
    private int typeEnd(int start) {
        int scan = start;
        if (!(identifier(scan) || PRIMITIVES.contains(word(scan)))) {
            return -1;
        }
        scan++;
        while (scan < limit) {
            if (word(scan).equals("<")) {
                int nesting = 1;
                scan++;
                while (scan < limit && nesting > 0) {
                    String token = word(scan++);
                    if (token.equals("<")) {
                        nesting++;
                    } else if (token.equals(">")) {
                        nesting--;
                    } else if (Set.of(";", "=", "{", "}").contains(token)) {
                        return -1;
                    }
                }
                if (nesting != 0) {
                    return -1;
                }
            } else if (word(scan).equals(".") && identifier(scan + 1)) {
                scan += 2;
            } else if (word(scan).equals("[") && word(scan + 1).equals("]")) {
                scan += 2;
            } else {
                break;
            }
        }
        return scan;
    }

    private Expression expression(int minimum) {
        if (++depth > 128) {
            problem("Expression nesting exceeds 128 levels.");
            depth--;
            return null;
        }
        try {
            Expression left = primary();
            if (left == null) {
                return null;
            }
            while (position < limit) {
                if (at("?") && minimum <= 0) {
                    position++;
                    Expression whenTrue = expression(0);
                    require(":");
                    Expression whenFalse = expression(0);
                    if (whenTrue == null || whenFalse == null) {
                        problem("Expected both branches of a conditional expression.");
                        break;
                    }
                    left =
                            node(
                                    Expression.Kind.OPERATION,
                                    "?:",
                                    left.nameSpan(),
                                    left.span().start().offset(),
                                    whenFalse.span().end().offset(),
                                    null,
                                    List.of(left, whenTrue, whenFalse));
                    continue;
                }
                String operator = operator();
                int precedence = precedence(operator);
                if (precedence < minimum) {
                    break;
                }
                position += operator.equals("instanceof") ? 1 : operator.length();
                if (operator.equals("->")) {
                    SourceSpan lambdaScope =
                            at("{")
                                    ? scopeOf(position, left.span())
                                    : expressionScope(left.span().start().offset());
                    bindLambda(left, lambdaScope);
                    if (at("{")) {
                        block(lambdaScope);
                        return node(
                                Expression.Kind.LAMBDA,
                                "->",
                                left.nameSpan(),
                                left.span().start().offset(),
                                endPrevious(),
                                null,
                                List.of(left));
                    }
                }
                Expression right =
                        expression(
                                precedence
                                        + (ASSIGNMENTS.contains(operator) || operator.equals("->")
                                                ? 0
                                                : 1));
                if (right == null) {
                    problem("Expected an expression after '" + operator + "'.");
                    right = unknown();
                }
                left =
                        node(
                                operator.equals("->")
                                        ? Expression.Kind.LAMBDA
                                        : Expression.Kind.OPERATION,
                                operator,
                                left.nameSpan(),
                                left.span().start().offset(),
                                right.span().end().offset(),
                                null,
                                List.of(left, right));
            }
            return left;
        } finally {
            depth--;
        }
    }

    private Expression primary() {
        if (position >= limit || Set.of(";", ",", ")", "]", "}", ":").contains(word(position))) {
            return null;
        }
        int start = position;
        Token token = tokens.get(position++);
        Expression result;
        if (token.value().equals("switch")) {
            unsupported("Switch expressions are not yet indexed.");
            if (at("(") && closing[position] >= 0) {
                position = closing[position] + 1;
            }
            if (at("{") && closing[position] >= 0) {
                position = closing[position] + 1;
            }
            return node(
                    Expression.Kind.UNKNOWN,
                    "switch",
                    source.span(token),
                    token.offset(),
                    endPrevious(),
                    null,
                    List.of());
        } else if (token.value().equals("new")) {
            int end = typeEnd(position);
            if (end < 0) {
                problem("Expected a type after 'new'.");
                return unknown();
            }
            String type = text(position, end).replaceAll("\\s+", "");
            SourceSpan typeSpan = span(position, end);
            position = end;
            List<Expression> arguments = at("(") ? arguments() : List.of();
            if (at("[")) {
                while (take("[")) {
                    add(expression(0));
                    require("]");
                }
                type += "[]";
            }
            result =
                    node(
                            Expression.Kind.NEW,
                            type,
                            typeSpan,
                            token.offset(),
                            endPrevious(),
                            null,
                            arguments);
            if (at("{")) {
                unsupported("Anonymous class and array initializer bodies are not yet indexed.");
                position = closing[position] >= 0 ? closing[position] + 1 : limit;
            }
        } else if (token.value().equals("(")) {
            // A typed lambda is a binding list, not a sequence of references.
            int close = closing[start];
            if (close >= 0 && word(close + 1).equals("-") && word(close + 2).equals(">")) {
                List<Expression> names = new ArrayList<>();
                while (position < close) {
                    int end = typeEnd(position);
                    String type = null;
                    if (end >= 0 && end < close && identifier(end)) {
                        type = text(position, end);
                        position = end;
                    }
                    Token name = tokens.get(position++);
                    Expression binding =
                            node(
                                    Expression.Kind.NAME,
                                    name.value(),
                                    source.span(name),
                                    name.offset(),
                                    source.endOffset(name),
                                    null,
                                    List.of());
                    names.add(binding);
                    // The scope is attached when the arrow is consumed.
                    if (type != null) {
                        variables.add(
                                new LocalBinding(
                                        name.value(),
                                        type,
                                        source.span(name),
                                        source.span(name),
                                        null));
                    }
                    take(",");
                }
                require(")");
                result =
                        node(
                                Expression.Kind.OPERATION,
                                "parameters",
                                source.span(token),
                                token.offset(),
                                endPrevious(),
                                null,
                                names);
            } else {
                int castEnd = typeEnd(position);
                boolean cast =
                        castEnd > position
                                && word(castEnd).equals(")")
                                && (PRIMITIVES.contains(word(position))
                                        || identifier(castEnd + 1)
                                        || word(castEnd + 1).equals("new"));
                if (cast) {
                    String type = text(position, castEnd);
                    position = castEnd + 1;
                    Expression operand = primary();
                    result =
                            node(
                                    Expression.Kind.OPERATION,
                                    "cast:" + type,
                                    source.span(token),
                                    token.offset(),
                                    endPrevious(),
                                    null,
                                    operand == null ? List.of() : List.of(operand));
                } else {
                    result = expression(0);
                    require(")");
                    if (result == null) {
                        result = unknown();
                    }
                }
            }
        } else if (Set.of("!", "~", "+", "-").contains(token.value())) {
            String operator = token.value();
            if ((operator.equals("+") || operator.equals("-")) && take(operator)) {
                operator += operator;
            }
            Expression operand = primary();
            result =
                    node(
                            Expression.Kind.OPERATION,
                            operator,
                            source.span(token),
                            token.offset(),
                            endPrevious(),
                            null,
                            operand == null ? List.of() : List.of(operand));
        } else if (token.type() == JavaTokenKind.IDENTIFIER
                || Set.of("this", "super").contains(token.value())
                || ((PRIMITIVES.contains(token.value()) || token.value().equals("void"))
                        && at(".")
                        && word(position + 1).equals("class"))) {
            result =
                    node(
                            Expression.Kind.NAME,
                            token.value(),
                            source.span(token),
                            token.offset(),
                            source.endOffset(token),
                            null,
                            List.of());
        } else if (Set.of(
                        JavaTokenKind.STRING_LITERAL,
                        JavaTokenKind.TEXT_BLOCK,
                        JavaTokenKind.CHARACTER_LITERAL,
                        JavaTokenKind.NUMBER_LITERAL,
                        JavaTokenKind.BOOLEAN_LITERAL,
                        JavaTokenKind.NULL_LITERAL)
                .contains(token.type())) {
            String type =
                    switch ((JavaTokenKind) token.type()) {
                        case STRING_LITERAL, TEXT_BLOCK -> "java.lang.String";
                        case CHARACTER_LITERAL -> "char";
                        case BOOLEAN_LITERAL -> "boolean";
                        case NULL_LITERAL -> "null";
                        default -> numberType(token.value());
                    };
            result =
                    node(
                            Expression.Kind.LITERAL,
                            type,
                            source.span(token),
                            token.offset(),
                            source.endOffset(token),
                            null,
                            List.of());
        } else {
            unsupported("Unsupported or incomplete expression: '" + token.value() + "'.");
            return unknown();
        }
        while (position < limit) {
            if (at("(")) {
                List<Expression> arguments = arguments();
                result =
                        node(
                                Expression.Kind.CALL,
                                result.name(),
                                result.nameSpan(),
                                result.span().start().offset(),
                                endPrevious(),
                                result.kind() == Expression.Kind.FIELD ? result.receiver() : null,
                                arguments);
            } else if (take(".") || take("::")) {
                boolean methodReference = word(position - 1).equals("::");
                if (at("<")) {
                    int nesting = 0;
                    do {
                        if (at("<")) {
                            nesting++;
                        } else if (at(">")) {
                            nesting--;
                        }
                        position++;
                    } while (position < limit && nesting > 0 && !at(";") && !at("}"));
                }
                if (!(identifier(position)
                        || at("new")
                        || at("class")
                        || at("this")
                        || at("super"))) {
                    problem("Expected a member name.");
                    break;
                }
                Token name = tokens.get(position++);
                result =
                        node(
                                methodReference
                                        ? Expression.Kind.METHOD_REFERENCE
                                        : Expression.Kind.FIELD,
                                name.value(),
                                source.span(name),
                                result.span().start().offset(),
                                source.endOffset(name),
                                result,
                                List.of());
            } else if (take("[")) {
                Expression subscript = expression(0);
                require("]");
                result =
                        node(
                                Expression.Kind.ARRAY_ACCESS,
                                "[]",
                                result.nameSpan(),
                                result.span().start().offset(),
                                endPrevious(),
                                result,
                                subscript == null ? List.of() : List.of(subscript));
            } else if ((at("+") && word(position + 1).equals("+"))
                    || (at("-") && word(position + 1).equals("-"))) {
                String operator = word(position) + word(position + 1);
                position += 2;
                result =
                        node(
                                Expression.Kind.OPERATION,
                                operator,
                                result.nameSpan(),
                                result.span().start().offset(),
                                endPrevious(),
                                null,
                                List.of(result));
            } else {
                break;
            }
        }
        return result;
    }

    private List<Expression> arguments() {
        require("(");
        List<Expression> arguments = new ArrayList<>();
        while (position < limit && !at(")") && !at(";") && !at("}")) {
            int before = position;
            Expression argument = expression(0);
            if (argument != null) {
                arguments.add(argument);
            }
            if (!take(",")) {
                break;
            }
            if (position == before) {
                position++;
            }
        }
        require(")");
        return arguments;
    }

    private void bindLambda(Expression parameters, SourceSpan scope) {
        List<Expression> names =
                parameters.name().equals("parameters")
                        ? parameters.arguments()
                        : List.of(parameters);
        for (Expression name : names) {
            String type = null;
            for (int index = variables.size() - 1; index >= 0; index--) {
                LocalBinding previous = variables.get(index);
                if (previous.nameSpan().equals(name.nameSpan())
                        && previous.scope().equals(previous.nameSpan())) {
                    type = previous.type();
                    variables.remove(index);
                    break;
                }
            }
            variables.add(new LocalBinding(name.name(), type, name.nameSpan(), scope, null));
        }
    }

    private void references(
            Expression expression,
            BodyReference.Role role,
            SymbolId owner,
            SymbolId type,
            List<BodyReference> result) {
        Expression.Kind kind = expression.kind();
        BodyReference.Role ownRole =
                switch (kind) {
                    case CALL -> BodyReference.Role.CALLS;
                    case NEW -> BodyReference.Role.INSTANTIATES;
                    case METHOD_REFERENCE -> BodyReference.Role.METHOD_REFERENCE;
                    default -> role;
                };
        if (Set.of(
                        Expression.Kind.NAME,
                        Expression.Kind.FIELD,
                        Expression.Kind.CALL,
                        Expression.Kind.NEW,
                        Expression.Kind.METHOD_REFERENCE)
                .contains(kind)) {
            result.add(new BodyReference(expression, ownRole, owner, type));
        }
        if (expression.receiver() != null) {
            references(expression.receiver(), BodyReference.Role.READS, owner, type, result);
        }
        for (int index = 0; index < expression.arguments().size(); index++) {
            if (kind == Expression.Kind.LAMBDA && index == 0) {
                continue;
            }
            BodyReference.Role childRole = BodyReference.Role.READS;
            if (kind == Expression.Kind.OPERATION && index == 0) {
                if (expression.name().equals("=")) {
                    childRole = BodyReference.Role.WRITES;
                } else if (ASSIGNMENTS.contains(expression.name())
                        || Set.of("++", "--").contains(expression.name())) {
                    childRole = BodyReference.Role.READS_WRITES;
                }
            }
            references(expression.arguments().get(index), childRole, owner, type, result);
        }
    }

    private String operator() {
        String combined = "";
        String found = "";
        for (int count = 0; count < 4 && position + count < limit; count++) {
            String next = word(position + count);
            if (next.length() != 1) {
                break;
            }
            if (count > 0
                    && source.endOffset(tokens.get(position + count - 1))
                            != tokens.get(position + count).offset()) {
                break;
            }
            combined += next;
            if (precedence(combined) >= 0) {
                found = combined;
            }
        }
        if (at("instanceof")) {
            return "instanceof";
        }
        return found;
    }

    private int precedence(String operator) {
        if (ASSIGNMENTS.contains(operator) || operator.equals("->")) {
            return 0;
        }
        return switch (operator) {
            case "||" -> 1;
            case "&&" -> 2;
            case "|" -> 3;
            case "^" -> 4;
            case "&" -> 5;
            case "==", "!=" -> 6;
            case "<", ">", "<=", ">=", "instanceof" -> 7;
            case "<<", ">>", ">>>" -> 8;
            case "+", "-" -> 9;
            case "*", "/", "%" -> 10;
            default -> -1;
        };
    }

    private SourceSpan scopeOf(int opening, SourceSpan fallback) {
        int close = closing[opening];
        return source.lines()
                .span(
                        tokens.get(opening).offset(),
                        close >= 0 ? source.endOffset(tokens.get(close)) : fallback.end().offset());
    }

    private SourceSpan controlScope(SourceSpan fallback) {
        int end = position;
        if (word(end).equals("(") && closing[end] >= 0) {
            end = closing[end] + 1;
        }
        if (word(end).equals("{") && closing[end] >= 0) {
            return source.lines()
                    .span(
                            tokens.get(position).offset(),
                            source.endOffset(tokens.get(closing[end])));
        }
        while (end < limit && !Set.of(";", "}").contains(word(end))) {
            end++;
        }
        return source.lines()
                .span(
                        tokens.get(position).offset(),
                        Math.min(fallback.end().offset(), source.endOffset(tokens.get(end))));
    }

    private SourceSpan expressionScope(int start) {
        int end = position;
        while (end < limit && !Set.of(",", ";", ")", "}").contains(word(end))) {
            if ((word(end).equals("(") || word(end).equals("[")) && closing[end] >= 0) {
                end = closing[end];
            }
            end++;
        }
        return source.lines().span(start, tokens.get(Math.min(end, limit)).offset());
    }

    private static String numberType(String literal) {
        String lower = literal.toLowerCase(Locale.ROOT);
        if (lower.endsWith("l")) {
            return "long";
        }
        if (lower.startsWith("0x") && !lower.contains("p") && !lower.contains(".")) {
            return "int";
        }
        if (lower.endsWith("f")) {
            return "float";
        }
        return lower.endsWith("d")
                        || lower.contains(".")
                        || lower.contains("e")
                        || lower.contains("p")
                ? "double"
                : "int";
    }

    private static boolean matches(String opening, String close) {
        return (opening.equals("(") && close.equals(")"))
                || (opening.equals("[") && close.equals("]"))
                || (opening.equals("{") && close.equals("}"));
    }

    private int tokenAt(int offset) {
        int low = 0;
        int high = tokens.size() - 1;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (tokens.get(middle).offset() < offset) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        return low;
    }

    private String word(int index) {
        return index < 0 || index >= tokens.size() ? "" : tokens.get(index).value();
    }

    private boolean identifier(int index) {
        return index >= 0 && index < limit && JavaTokenKind.IDENTIFIER.matches(tokens.get(index));
    }

    private boolean at(String word) {
        return position < limit && word(position).equals(word);
    }

    private boolean take(String word) {
        if (at(word)) {
            position++;
            return true;
        }
        return false;
    }

    private void require(String word) {
        if (!take(word)) {
            problem("Expected '" + word + "'.");
        }
    }

    private String text(int start, int end) {
        return source.subSequence(tokens.get(start).offset(), source.endOffset(tokens.get(end - 1)))
                .toString();
    }

    private SourceSpan span(int start, int end) {
        return source.lines()
                .span(
                        tokens.get(start).offset(),
                        source.endOffset(tokens.get(Math.max(start, end - 1))));
    }

    private int endPrevious() {
        return source.endOffset(tokens.get(Math.max(0, position - 1)));
    }

    private Expression node(
            Expression.Kind kind,
            String name,
            SourceSpan nameSpan,
            int start,
            int end,
            Expression receiver,
            List<Expression> arguments) {
        return new Expression(
                kind,
                name,
                nameSpan,
                source.lines().span(start, Math.max(start, end)),
                receiver,
                arguments);
    }

    private Expression unknown() {
        Token token = tokens.get(Math.min(position, tokens.size() - 1));
        return new Expression(
                Expression.Kind.UNKNOWN,
                "",
                source.span(token),
                source.span(token),
                null,
                List.of());
    }

    private void add(Expression expression) {
        if (expression != null) {
            expressions.add(expression);
        }
    }

    private void problem(String message) {
        Token token = tokens.get(Math.min(position, tokens.size() - 1));
        problems.add(
                new ParseProblem(
                        ProblemKind.UNEXPECTED_TOKEN,
                        message,
                        source.span(token),
                        ParseProblem.Phase.BODY));
    }

    private void unsupported(String message) {
        Token token = tokens.get(Math.min(position, tokens.size() - 1));
        problems.add(
                new ParseProblem(
                        ProblemKind.UNSUPPORTED_SYNTAX,
                        message,
                        source.span(token),
                        ParseProblem.Phase.BODY));
    }
}
