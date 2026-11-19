package org.jmouse.java.parser.java27;

import org.jmouse.java.parser.extension.JavaSourceExtension;
import org.jmouse.java.parser.extension.JavaSourceExtensionDescriptor;
import org.jmouse.java.parser.extension.JavaSourceExtensionNames;
import org.jmouse.java.parser.extension.JavaSourceLanguage;
import org.jmouse.java.parser.grammar.JavaParseRoles;
import org.jmouse.java.parser.grammar.statement.JavaStatementRoles;
import org.jmouse.java.parser.grammar.unit.JavaUnitRoles;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.lexer.JavaLexicalMatch;
import org.jmouse.java.parser.projection.JavaStatementProjection;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class Java27SourceExtension implements JavaSourceExtension {
    @Override
    public JavaSourceExtensionDescriptor descriptor() {
        return new JavaSourceExtensionDescriptor(
                Java27Names.EXTENSION,
                Java27Names.REVISION,
                Set.of(JavaSourceExtensionNames.STANDARD),
                Set.of(Java27Names.FEATURE));
    }

    @Override
    public void configure(JavaSourceLanguage.Builder builder) {
        builder.profile(Java27Example.profile());
        builder.keywords(List.of(Java27Keyword.values()));
        builder.lexicalRule(
                input -> {
                    if (!input.startsWith(Java27Names.VALUE_PREFIX)) {
                        return Optional.empty();
                    }
                    return Optional.of(
                            new JavaLexicalMatch(
                                    Java27Names.VALUE_PREFIX.length(),
                                    output ->
                                            output.token(
                                                    Java27Token.VALUE_PREFIX,
                                                    Java27Names.VALUE_PREFIX,
                                                    0,
                                                    Java27Names.VALUE_PREFIX.length())));
                });
        builder.rules()
                .bind(
                        Java27Roles.INSPECT,
                        Java27InspectParser.class.getName(),
                        new Java27InspectParser());
        builder.rules()
                .bind(
                        Java27Roles.VALUE,
                        Java27ValueParser.class.getName(),
                        new Java27ValueParser());
        builder.rules()
                .bind(
                        Java27Roles.IMPORT,
                        Java27BundleImportParser.class.getName(),
                        new Java27BundleImportParser());
        builder.prepend(
                JavaStatementRoles.STATEMENT,
                Java27Names.STATEMENT_SELECTION,
                input -> input.matches(Java27Keyword.INSPECT),
                (input, session) -> session.parse(Java27Roles.INSPECT, input));
        builder.prepend(
                JavaParseRoles.PRIMARY,
                Java27Names.PRIMARY_SELECTION,
                input -> input.matches(Java27Token.VALUE_PREFIX),
                (input, session) -> session.parse(Java27Roles.VALUE, input));
        builder.prepend(
                JavaUnitRoles.IMPORT,
                Java27Names.IMPORT_SELECTION,
                input ->
                        input.matches(JavaKeyword.IMPORT)
                                && input.cursor()
                                        .lookAt(1)
                                        .value()
                                        .equals(Java27Keyword.BUNDLE.spelling()),
                (input, session) -> session.parse(Java27Roles.IMPORT, input));
        builder.projections()
                .expressions()
                .bind(
                        Java27ValueNode.KIND,
                        Java27ValueProjection.class.getName(),
                        new Java27ValueProjection());
        builder.projections()
                .imports()
                .bind(
                        Java27BundleImportNode.KIND,
                        Java27ImportProjection.class.getName(),
                        new Java27ImportProjection());
        builder.projections()
                .statements()
                .bind(
                        Java27InspectNode.KIND,
                        Java27SourceExtension.class.getName(),
                        (node, context) -> JavaStatementProjection.TRAVERSE);
    }
}
