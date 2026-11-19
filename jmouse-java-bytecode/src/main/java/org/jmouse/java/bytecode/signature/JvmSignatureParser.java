package org.jmouse.java.bytecode.signature;

import org.jmouse.el.lexer.Lexer;
import org.jmouse.el.parser.ParseRuleRegistry;
import org.jmouse.el.parser.ParsingBudget;
import org.jmouse.java.bytecode.JavaBytecodeLimits;

/** Full JVMS generic signature grammar, independent from reflection and host class loading. */
public final class JvmSignatureParser {
    private final JvmTextGrammar grammar;

    public JvmSignatureParser() {
        this(new JvmTextLexer(), standardRules(), JavaBytecodeLimits.standard().parsing());
    }

    public JvmSignatureParser(Lexer lexer, ParseRuleRegistry rules, ParsingBudget budget) {
        grammar = new JvmTextGrammar(lexer, rules, budget);
    }

    public static ParseRuleRegistry standardRules() {
        ParseRuleRegistry rules = JvmDescriptorParser.standardRules();
        rules.bind(
                JvmSignatureRoles.TYPE,
                JvmSignatureTypeParser.class.getName(),
                new JvmSignatureTypeParser());
        rules.bind(
                JvmSignatureRoles.REFERENCE,
                JvmReferenceSignatureParser.class.getName(),
                new JvmReferenceSignatureParser());
        rules.bind(
                JvmSignatureRoles.CLASS_TYPE,
                JvmClassTypeSignatureParser.class.getName(),
                new JvmClassTypeSignatureParser());
        rules.bind(
                JvmSignatureRoles.SEGMENT,
                JvmClassTypeSegmentParser.class.getName(),
                new JvmClassTypeSegmentParser());
        rules.bind(
                JvmSignatureRoles.ARGUMENT,
                JvmTypeArgumentParser.class.getName(),
                new JvmTypeArgumentParser());
        rules.bind(
                JvmSignatureRoles.TYPE_PARAMETER,
                JvmTypeParameterParser.class.getName(),
                new JvmTypeParameterParser());
        rules.bind(
                JvmSignatureRoles.THROWS,
                JvmThrowsSignatureParser.class.getName(),
                new JvmThrowsSignatureParser());
        rules.bind(
                JvmSignatureRoles.CLASS,
                JvmClassSignatureParser.class.getName(),
                new JvmClassSignatureParser());
        rules.bind(
                JvmSignatureRoles.METHOD,
                JvmMethodSignatureParser.class.getName(),
                new JvmMethodSignatureParser());
        return rules;
    }

    public JvmReferenceTypeSignatureNode field(String text) {
        return field(JvmTextNames.SOURCE, text);
    }

    public JvmReferenceTypeSignatureNode field(String name, String text) {
        return grammar.parse(name, text, JvmSignatureRoles.REFERENCE);
    }

    public JvmMethodSignatureNode method(String text) {
        return method(JvmTextNames.SOURCE, text);
    }

    public JvmMethodSignatureNode method(String name, String text) {
        return grammar.parse(name, text, JvmSignatureRoles.METHOD);
    }

    public JvmClassSignatureNode classSignature(String text) {
        return classSignature(JvmTextNames.SOURCE, text);
    }

    public JvmClassSignatureNode classSignature(String name, String text) {
        return grammar.parse(name, text, JvmSignatureRoles.CLASS);
    }
}
