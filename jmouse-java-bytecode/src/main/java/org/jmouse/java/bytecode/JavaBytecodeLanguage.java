package org.jmouse.java.bytecode;

import org.jmouse.el.lexer.Lexer;
import org.jmouse.el.parser.ParseRole;
import org.jmouse.el.parser.ParseRule;
import org.jmouse.el.parser.ParseRuleRegistry;
import org.jmouse.el.translate.NodeOperationRegistry;
import org.jmouse.java.bytecode.grammar.JavaAnnotationParser;
import org.jmouse.java.bytecode.grammar.JavaAnnotationValueParser;
import org.jmouse.java.bytecode.grammar.JavaAttributeInput;
import org.jmouse.java.bytecode.grammar.JavaAttributeParser;
import org.jmouse.java.bytecode.grammar.JavaAttributePolicy;
import org.jmouse.java.bytecode.grammar.JavaBytecodeRoles;
import org.jmouse.java.bytecode.grammar.JavaConstantInput;
import org.jmouse.java.bytecode.grammar.JavaConstantParser;
import org.jmouse.java.bytecode.grammar.JavaInstructionParser;
import org.jmouse.java.bytecode.grammar.JavaStackMapFrameParser;
import org.jmouse.java.bytecode.grammar.JavaTypeAnnotationParser;
import org.jmouse.java.bytecode.grammar.JavaVerificationTypeParser;
import org.jmouse.java.bytecode.grammar.JvmStandardAttributePolicy;
import org.jmouse.java.bytecode.node.attribute.JavaAttributeNode;
import org.jmouse.java.bytecode.node.constant.JavaConstantNode;
import org.jmouse.java.bytecode.signature.JvmSignatureParser;
import org.jmouse.java.bytecode.signature.JvmTextLexer;
import org.jmouse.java.bytecode.validation.JavaBytecodeValidation;
import org.jmouse.java.bytecode.validation.JvmValidationContext;
import org.jmouse.java.bytecode.validation.JvmValidationOutcome;
import org.jmouse.java.bytecode.vocabulary.JvmAttributeKind;
import org.jmouse.java.bytecode.vocabulary.JvmConstantKind;
import org.jmouse.java.bytecode.vocabulary.JvmConstantTag;
import org.jmouse.java.model.version.BytecodeProfile;
import org.jmouse.java.model.version.JavaSpecificationCatalog;
import org.jmouse.java.model.version.PreviewPolicy;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable binary grammar snapshot. Unknown tags require an explicit contributed decoder. */
public final class JavaBytecodeLanguage {

    private final BytecodeProfile profile;
    private final Lexer textLexer;
    private final JavaAttributePolicy attributePolicy;
    private final NodeOperationRegistry<JvmValidationContext, JvmValidationOutcome> validations;
    private final ParseRuleRegistry rules;
    private final Map<String, ParseRole<JavaAttributeInput, JavaAttributeNode>> attributes;
    private final Map<Integer, ParseRole<JavaConstantInput, JavaConstantNode>> constants;

    private JavaBytecodeLanguage(Builder builder) {
        profile = Objects.requireNonNull(builder.profile, "profile");
        textLexer = Objects.requireNonNull(builder.textLexer, "textLexer");
        attributePolicy = Objects.requireNonNull(builder.attributePolicy, "attributePolicy");
        validations = builder.validations.snapshot();
        rules = builder.rules.snapshot();
        attributes = Map.copyOf(builder.attributes);
        constants = Map.copyOf(builder.constants);
    }

    public static JavaBytecodeLanguage standard() {
        return Standard.INSTANCE;
    }

    public static Builder builder() {
        return standard().toBuilder();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public BytecodeProfile profile() {
        return profile;
    }

    public Lexer textLexer() {
        return textLexer;
    }

    public JavaAttributePolicy attributePolicy() {
        return attributePolicy;
    }

    public NodeOperationRegistry<JvmValidationContext, JvmValidationOutcome> validations() {
        return validations;
    }

    public ParseRuleRegistry rules() {
        return rules;
    }

    public Optional<ParseRole<JavaAttributeInput, JavaAttributeNode>> attribute(String name) {
        return Optional.ofNullable(attributes.get(name));
    }

    public ParseRole<JavaConstantInput, JavaConstantNode> constant(int tag) {
        ParseRole<JavaConstantInput, JavaConstantNode> role = constants.get(tag);
        if (role == null) {
            throw new IllegalArgumentException(
                    "No registered class-file constant decoder for tag " + tag);
        }
        return role;
    }

    public static final class Builder {
        private BytecodeProfile profile;
        private Lexer textLexer;
        private JavaAttributePolicy attributePolicy;
        private final NodeOperationRegistry<JvmValidationContext, JvmValidationOutcome> validations;
        private final ParseRuleRegistry rules;
        private final Map<String, ParseRole<JavaAttributeInput, JavaAttributeNode>> attributes;
        private final Map<Integer, ParseRole<JavaConstantInput, JavaConstantNode>> constants;

        private Builder() {
            JavaSpecificationCatalog catalog = JavaSpecificationCatalog.standard();
            profile =
                    new BytecodeProfile(
                            catalog.defaultRelease(), PreviewPolicy.disabled(), catalog);
            textLexer = new JvmTextLexer();
            attributePolicy = new JvmStandardAttributePolicy();
            validations = JavaBytecodeValidation.standardRules();
            rules = JvmSignatureParser.standardRules();
            attributes = new HashMap<>();
            constants = new HashMap<>();
        }

        private Builder(JavaBytecodeLanguage language) {
            profile = language.profile;
            textLexer = language.textLexer;
            attributePolicy = language.attributePolicy;
            validations = language.validations.fork();
            rules = language.rules.fork();
            attributes = new HashMap<>(language.attributes);
            constants = new HashMap<>(language.constants);
        }

        public Builder profile(BytecodeProfile profile) {
            this.profile = Objects.requireNonNull(profile, "profile");
            return this;
        }

        public Builder textLexer(Lexer textLexer) {
            this.textLexer = Objects.requireNonNull(textLexer, "textLexer");
            return this;
        }

        public NodeOperationRegistry<JvmValidationContext, JvmValidationOutcome> validations() {
            return validations;
        }

        public ParseRuleRegistry rules() {
            return rules;
        }

        public Builder attributePolicy(JavaAttributePolicy policy) {
            attributePolicy = Objects.requireNonNull(policy, "policy");
            return this;
        }

        public Builder attribute(
                String name,
                String identity,
                ParseRule<JavaAttributeInput, ? extends JavaAttributeNode> decoder) {
            ParseRole<JavaAttributeInput, JavaAttributeNode> role =
                    new ParseRole<>(
                            JavaBytecodeNames.ATTRIBUTE_ROLE + "." + name,
                            JavaAttributeInput.class,
                            JavaAttributeNode.class);
            if (attributes.containsKey(name)) {
                throw new IllegalArgumentException("Duplicate attribute decoder: " + name);
            }
            rules.bind(role, identity, (input, session) -> decoder.parse(input, session));
            attributes.put(name, role);
            return this;
        }

        public Builder constant(
                JvmConstantKind kind,
                String identity,
                ParseRule<JavaConstantInput, ? extends JavaConstantNode> decoder) {
            ParseRole<JavaConstantInput, JavaConstantNode> role =
                    new ParseRole<>(
                            JavaBytecodeNames.CONSTANT_ROLE + "." + kind.name(),
                            JavaConstantInput.class,
                            JavaConstantNode.class);
            if (constants.containsKey(kind.code())) {
                throw new IllegalArgumentException("Duplicate constant decoder: " + kind.code());
            }
            rules.bind(role, identity, (input, session) -> decoder.parse(input, session));
            constants.put(kind.code(), role);
            return this;
        }

        public JavaBytecodeLanguage build() {
            return new JavaBytecodeLanguage(this);
        }
    }

    private static final class Standard {
        private static final JavaBytecodeLanguage INSTANCE = create();

        private static JavaBytecodeLanguage create() {
            Builder builder = new Builder();
            builder.rules.bind(
                    JavaBytecodeRoles.STACK_MAP_FRAME,
                    JavaStackMapFrameParser.class.getName(),
                    new JavaStackMapFrameParser());
            builder.rules.bind(
                    JavaBytecodeRoles.VERIFICATION_TYPE,
                    JavaVerificationTypeParser.class.getName(),
                    new JavaVerificationTypeParser());
            for (JvmConstantTag tag : JvmConstantTag.values()) {
                builder.constant(tag, JavaConstantParser.class.getName(), new JavaConstantParser());
            }
            for (JvmAttributeKind kind : JvmAttributeKind.values()) {
                builder.attribute(
                        kind.encodedName(),
                        JavaAttributeParser.class.getName(),
                        new JavaAttributeParser(kind));
            }
            builder.rules.bind(
                    JavaBytecodeRoles.INSTRUCTION,
                    JavaInstructionParser.class.getName(),
                    new JavaInstructionParser());
            builder.rules.bind(
                    JavaBytecodeRoles.ANNOTATION,
                    JavaAnnotationParser.class.getName(),
                    new JavaAnnotationParser());
            builder.rules.bind(
                    JavaBytecodeRoles.ANNOTATION_VALUE,
                    JavaAnnotationValueParser.class.getName(),
                    new JavaAnnotationValueParser());
            builder.rules.bind(
                    JavaBytecodeRoles.TYPE_ANNOTATION,
                    JavaTypeAnnotationParser.class.getName(),
                    new JavaTypeAnnotationParser());
            return builder.build();
        }
    }
}
