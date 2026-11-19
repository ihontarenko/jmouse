package org.jmouse.java.parser.extension;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jmouse.el.lexer.Lexer;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.ParseRole;
import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseRuleRegistry;
import org.jmouse.java.model.version.JavaFeatureId;
import org.jmouse.java.model.version.JavaSourceFeatures;
import org.jmouse.java.model.version.JavaSourceProfile;
import org.jmouse.java.model.version.JavaSpecificationCatalog;
import org.jmouse.java.parser.JavaSyntaxParser;
import org.jmouse.java.parser.lexer.JavaKeywordDefinition;
import org.jmouse.java.parser.lexer.JavaLexer;
import org.jmouse.java.parser.lexer.JavaLexicalProfile;
import org.jmouse.java.parser.lexer.JavaLexicalRule;

/** Frozen language configuration. Extending it produces an independent language, never mutating a running parser. */
public final class JavaSourceLanguage {
    private static final JavaSourceExtensionDescriptor STANDARD = new JavaSourceExtensionDescriptor(
            JavaSourceExtensionNames.STANDARD, JavaSourceExtensionNames.STANDARD_REVISION, Set.of(),
            Set.of(JavaSourceFeatures.MODULES, JavaSourceFeatures.UNDERSCORE_KEYWORD,
                    JavaSourceFeatures.LOCAL_VARIABLE_INFERENCE, JavaSourceFeatures.LAMBDA_VAR_PARAMETERS,
                    JavaSourceFeatures.SWITCH_EXPRESSIONS, JavaSourceFeatures.TEXT_BLOCKS,
                    JavaSourceFeatures.RECORD_DECLARATIONS, JavaSourceFeatures.INSTANCEOF_PATTERNS,
                    JavaSourceFeatures.SEALED_TYPES, JavaSourceFeatures.SWITCH_PATTERNS,
                    JavaSourceFeatures.RECORD_PATTERNS));
    private final JavaSourceProfile profile;
    private final JavaLexicalProfile lexical;
    private final ParseRuleRegistry rules;
    private final List<JavaSourceExtensionDescriptor> extensions;
    private final BiFunction<JavaSourceProfile, JavaLexicalProfile, Lexer> lexerFactory;

    public JavaSourceLanguage(JavaSourceProfile profile, JavaLexicalProfile lexical, ParseRuleRegistry rules) {
        this(profile, lexical, rules, List.of(STANDARD), JavaLexer::new);
    }

    private JavaSourceLanguage(JavaSourceProfile profile, JavaLexicalProfile lexical, ParseRuleRegistry rules,
                               List<JavaSourceExtensionDescriptor> extensions,
                               BiFunction<JavaSourceProfile, JavaLexicalProfile, Lexer> lexerFactory) {
        this.profile = Objects.requireNonNull(profile, "profile");
        this.lexical = Objects.requireNonNull(lexical, "lexical");
        this.rules = Objects.requireNonNull(rules, "rules").snapshot();
        this.extensions = List.copyOf(extensions);
        this.lexerFactory = Objects.requireNonNull(lexerFactory, "lexerFactory");
        validate(this.profile, this.lexical, this.extensions);
    }

    private static void validate(JavaSourceProfile profile, JavaLexicalProfile lexical,
                                 List<JavaSourceExtensionDescriptor> extensions) {
        for (JavaSourceExtensionDescriptor extension : extensions) {
            for (JavaFeatureId feature : extension.features()) {
                profile.specification().feature(feature);
            }
        }
        for (JavaKeywordDefinition keyword : lexical.keywords().definitions()) {
            keyword.typeNameRestriction().ifPresent(profile.specification()::feature);
        }
    }

    public static JavaSourceLanguage standard() {
        return Standard.INSTANCE;
    }

    public static Builder builder() {
        return new Builder(JavaSourceProfile.standard(JavaSpecificationCatalog.standard().defaultRelease()),
                JavaLexicalProfile.standard(), JavaSyntaxParser.standardRules(), List.of(STANDARD), JavaLexer::new);
    }

    public Builder toBuilder() {
        return new Builder(profile, lexical, rules.fork(), extensions, lexerFactory);
    }

    public JavaSourceProfile profile() {
        return profile;
    }

    public JavaLexicalProfile lexical() {
        return lexical;
    }

    public ParseRuleRegistry rules() {
        return rules;
    }

    public List<JavaSourceExtensionDescriptor> extensions() {
        return extensions;
    }

    public Lexer newLexer() {
        return Objects.requireNonNull(lexerFactory.apply(profile, lexical), "lexer");
    }

    public static final class Builder {
        private enum State { BUILDING, FROZEN }

        private final Thread owner = Thread.currentThread();
        private State state = State.BUILDING;
        private JavaSourceProfile profile;
        private JavaLexicalProfile lexical;
        private ParseRuleRegistry rules;
        private List<JavaSourceExtensionDescriptor> extensions;
        private BiFunction<JavaSourceProfile, JavaLexicalProfile, Lexer> lexerFactory;

        private Builder(JavaSourceProfile profile, JavaLexicalProfile lexical, ParseRuleRegistry rules,
                        List<JavaSourceExtensionDescriptor> extensions,
                        BiFunction<JavaSourceProfile, JavaLexicalProfile, Lexer> lexerFactory) {
            this.profile = profile;
            this.lexical = lexical;
            this.rules = rules;
            this.extensions = new ArrayList<>(extensions);
            this.lexerFactory = lexerFactory;
        }

        public Builder profile(JavaSourceProfile profile) {
            requireBuilding();
            this.profile = Objects.requireNonNull(profile, "profile");
            return this;
        }

        public Builder keywords(Collection<? extends JavaKeywordDefinition> keywords) {
            requireBuilding();
            lexical = lexical.withKeywords(keywords);
            return this;
        }

        public Builder replaceKeyword(JavaKeywordDefinition expected, JavaKeywordDefinition replacement) {
            requireBuilding();
            lexical = lexical.replaceKeyword(expected, replacement);
            return this;
        }

        public Builder lexicalRule(JavaLexicalRule rule) {
            requireBuilding();
            lexical = lexical.withRule(Objects.requireNonNull(rule, "rule"));
            return this;
        }

        public Builder lexicalProfile(JavaLexicalProfile lexical) {
            requireBuilding();
            this.lexical = Objects.requireNonNull(lexical, "lexical");
            return this;
        }

        public Builder lexerFactory(BiFunction<JavaSourceProfile, JavaLexicalProfile, Lexer> factory) {
            requireBuilding();
            lexerFactory = Objects.requireNonNull(factory, "factory");
            return this;
        }

        public ParseRuleRegistry rules() {
            requireBuilding();
            return rules;
        }

        /** Adds an alternative while retaining the exact previously registered rule as its fallback. */
        public <I, N extends Node> Builder prepend(ParseRole<I, N> role, String identity, Predicate<I> accepts,
                                                  ParseRule<I, ? extends N> contribution) {
            requireBuilding();
            Objects.requireNonNull(accepts, "accepts");
            Objects.requireNonNull(contribution, "contribution");
            rules.decorate(role, rules.identity(role), identity, previous -> (input, session) ->
                    accepts.test(input) ? contribution.parse(input, session) : previous.parse(input, session));
            return this;
        }

        /** Installation is transactional: a failed contributor cannot leave half its configuration behind. */
        public Builder install(JavaSourceExtension extension) {
            requireBuilding();
            Objects.requireNonNull(extension, "extension");
            JavaSourceExtensionDescriptor descriptor = Objects.requireNonNull(extension.descriptor(), "descriptor");
            if (extensions.stream().anyMatch(installed -> installed.identity().equals(descriptor.identity()))) {
                throw new IllegalArgumentException("Duplicate source extension: " + descriptor.identity());
            }
            for (String dependency : descriptor.requires()) {
                if (extensions.stream().noneMatch(installed -> installed.identity().equals(dependency))) {
                    throw new IllegalArgumentException("Missing source extension dependency: " + dependency);
                }
            }
            Builder staged = new Builder(profile, lexical, rules.fork(), extensions, lexerFactory);
            staged.extensions.add(descriptor);
            extension.configure(staged);
            staged.requireBuilding();
            validate(staged.profile, staged.lexical, staged.extensions);
            profile = staged.profile;
            lexical = staged.lexical;
            rules = staged.rules;
            extensions = staged.extensions;
            lexerFactory = staged.lexerFactory;
            return this;
        }

        public JavaSourceLanguage build() {
            requireBuilding();
            JavaSourceLanguage language = new JavaSourceLanguage(profile, lexical, rules, extensions, lexerFactory);
            state = State.FROZEN;
            return language;
        }

        private void requireBuilding() {
            if (state != State.BUILDING || Thread.currentThread() != owner) {
                throw new IllegalStateException("A language builder is confined to its construction thread until frozen");
            }
        }
    }

    private static final class Standard {
        private static final JavaSourceLanguage INSTANCE = builder().build();
    }
}
