package org.jmouse.java.bytecode.signature;

import org.jmouse.el.lexer.Lexer;
import org.jmouse.el.parser.ParseRuleRegistry;
import org.jmouse.el.parser.ParsingBudget;
import org.jmouse.java.bytecode.JavaBytecodeLimits;
import org.jmouse.java.model.descriptor.JavaMethodDescriptor;
import org.jmouse.java.model.descriptor.JavaTypeDescriptor;

/** Public descriptor boundary; grammar rules and the lexer can be replaced independently. */
public final class JvmDescriptorParser {
    private final JvmTextGrammar grammar;

    public JvmDescriptorParser() {
        this(new JvmTextLexer(), standardRules(), JavaBytecodeLimits.standard().parsing());
    }

    public JvmDescriptorParser(Lexer lexer, ParseRuleRegistry rules, ParsingBudget budget) {
        grammar = new JvmTextGrammar(lexer, rules, budget);
    }

    public static ParseRuleRegistry standardRules() {
        ParseRuleRegistry rules = new ParseRuleRegistry();
        rules.bind(
                JvmDescriptorRoles.FIELD,
                JvmDescriptorTypeParser.class.getName(),
                new JvmDescriptorTypeParser(JvmDescriptorTypeParser.Role.FIELD));
        rules.bind(
                JvmDescriptorRoles.RETURN,
                JvmDescriptorTypeParser.class.getName(),
                new JvmDescriptorTypeParser(JvmDescriptorTypeParser.Role.RETURN));
        rules.bind(
                JvmDescriptorRoles.METHOD,
                JvmMethodDescriptorParser.class.getName(),
                new JvmMethodDescriptorParser());
        return rules;
    }

    public JavaTypeDescriptor field(String text) {
        return parseField(JvmTextNames.SOURCE, text).descriptor();
    }

    public JavaMethodDescriptor method(String text) {
        return parseMethod(JvmTextNames.SOURCE, text).descriptor();
    }

    public JvmDescriptorTypeNode parseField(String name, String text) {
        return grammar.parse(name, text, JvmDescriptorRoles.FIELD);
    }

    public JvmMethodDescriptorNode parseMethod(String name, String text) {
        return grammar.parse(name, text, JvmDescriptorRoles.METHOD);
    }
}
