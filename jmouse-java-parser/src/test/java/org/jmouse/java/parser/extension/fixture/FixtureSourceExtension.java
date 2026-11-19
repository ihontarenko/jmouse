package org.jmouse.java.parser.extension.fixture;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.jmouse.el.parser.ParseRole;
import org.jmouse.java.model.version.ClassFileVersion;
import org.jmouse.java.model.version.JavaFeatureDefinition;
import org.jmouse.java.model.version.JavaFeatureId;
import org.jmouse.java.model.version.JavaRelease;
import org.jmouse.java.model.version.JavaSourceProfile;
import org.jmouse.java.model.version.JavaSpecificationCatalog;
import org.jmouse.java.model.version.PreviewPolicy;
import org.jmouse.java.parser.extension.JavaSourceExtension;
import org.jmouse.java.parser.extension.JavaSourceExtensionNames;
import org.jmouse.java.parser.extension.JavaSourceExtensionDescriptor;
import org.jmouse.java.parser.extension.JavaSourceLanguage;
import org.jmouse.java.parser.grammar.JavaParseRoles;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.grammar.statement.JavaStatementRoles;
import org.jmouse.java.parser.grammar.unit.JavaUnitRoles;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.lexer.JavaLexicalMatch;
import org.jmouse.java.parser.lexer.JavaToken;

/** Synthetic syntax demonstrates extension mechanics; it does not claim actual Java 25 syntax support. */
final class FixtureSourceExtension implements JavaSourceExtension {
    private static final ParseRole<JavaParsingInput, FixtureTraceNode> TRACE =
            new ParseRole<>(FixtureNames.TRACE_ROLE, JavaParsingInput.class, FixtureTraceNode.class);

    @Override
    public JavaSourceExtensionDescriptor descriptor() {
        return new JavaSourceExtensionDescriptor(FixtureNames.EXTENSION, FixtureNames.REVISION,
                Set.of(JavaSourceExtensionNames.STANDARD), Set.of(FixtureNames.TRACE_FEATURE));
    }

    @Override
    public void configure(JavaSourceLanguage.Builder builder) {
        builder.profile(profile());
        builder.keywords(List.of(FixtureKeyword.values()));
        builder.lexicalRule(input -> {
            if (!input.startsWith(FixtureNames.TRACE_PREFIX)) {
                return Optional.empty();
            }
            return Optional.of(new JavaLexicalMatch(FixtureNames.TRACE_PREFIX.length(), output ->
                    output.token(FixtureToken.TRACE_PREFIX, FixtureNames.TRACE_PREFIX, 0, FixtureNames.TRACE_PREFIX.length())));
        });
        builder.rules().bind(TRACE, FixtureNames.TRACE_BINDING, (input, session) -> {
            int start = input.offset();
            input.requireFeature(FixtureNames.TRACE_FEATURE);
            if (!input.take(FixtureKeyword.TRACE, session)) {
                input.expect(FixtureToken.TRACE_PREFIX, session);
            }
            FixtureTraceNode node = new FixtureTraceNode(input.name(), input.point());
            node.setExpression(session.parse(JavaParseRoles.EXPRESSION, input));
            input.expect(JavaToken.SEMICOLON, session);
            return input.finish(node, start);
        });
        builder.prepend(JavaStatementRoles.STATEMENT, FixtureNames.TRACE_SELECTION,
                input -> input.matches(FixtureKeyword.TRACE) || input.matches(FixtureToken.TRACE_PREFIX),
                (input, session) -> session.parse(TRACE, input));
        builder.prepend(JavaUnitRoles.IMPORT, FixtureNames.IMPORT_SELECTION,
                input -> input.matches(JavaKeyword.IMPORT)
                        && input.cursor().lookAt(1).value().equals(FixtureKeyword.BUNDLE.spelling()),
                (input, session) -> {
                    int start = input.offset();
                    input.expect(JavaKeyword.IMPORT, session);
                    input.expect(FixtureKeyword.BUNDLE, session);
                    FixtureBundleImportNode node = new FixtureBundleImportNode(input.name(), input.point());
                    node.setName(session.parse(JavaParseRoles.QUALIFIED_NAME, input));
                    input.expect(JavaToken.SEMICOLON, session);
                    return input.finish(node, start);
                });
    }

    private JavaSourceProfile profile() {
        JavaSpecificationCatalog baseline = JavaSpecificationCatalog.standard();
        JavaRelease release = new JavaRelease(FixtureNames.SOURCE_RELEASE);
        Map<JavaRelease, ClassFileVersion> releases = new HashMap<>();
        for (JavaRelease known : baseline.knownReleases()) {
            releases.put(known, baseline.classFileVersion(known));
        }
        releases.put(release, new ClassFileVersion(FixtureNames.CLASSFILE_MAJOR, 0));
        Map<JavaFeatureId, JavaFeatureDefinition> features = new HashMap<>(baseline.features());
        features.put(FixtureNames.TRACE_FEATURE, new JavaFeatureDefinition(FixtureNames.TRACE_FEATURE,
                Optional.of(release), Set.of(), Optional.empty()));
        JavaSpecificationCatalog catalog = new JavaSpecificationCatalog(FixtureNames.CATALOG, release,
                releases, features, baseline.classFileRules());
        return new JavaSourceProfile(release, PreviewPolicy.disabled(), catalog);
    }
}
