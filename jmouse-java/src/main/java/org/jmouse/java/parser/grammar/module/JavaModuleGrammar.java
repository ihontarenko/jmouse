package org.jmouse.java.parser.grammar.module;

import java.util.Map;
import java.util.Set;
import org.jmouse.el.parser.ParseRuleRegistry;
import org.jmouse.java.parser.grammar.JavaModifiersParser;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.node.module.JavaExportsDirectiveNode;
import org.jmouse.java.parser.node.module.JavaOpensDirectiveNode;

public final class JavaModuleGrammar {
    private JavaModuleGrammar() {
    }

    public static void install(ParseRuleRegistry rules) {
        rules.bind(JavaModuleRoles.MODULE, JavaModuleDeclarationParser.class.getName(), new JavaModuleDeclarationParser());
        rules.bind(JavaModuleRoles.REQUIRES, JavaRequiresDirectiveParser.class.getName(), new JavaRequiresDirectiveParser());
        rules.bind(JavaModuleRoles.REQUIRES_MODIFIERS, JavaModifiersParser.class.getName(),
                new JavaModifiersParser(Set.of(JavaKeyword.STATIC, JavaKeyword.TRANSITIVE)));
        rules.bind(JavaModuleRoles.EXPORTS, JavaPackageAccessDirectiveParser.class.getName() + "#" + JavaKeyword.EXPORTS.name(),
                new JavaPackageAccessDirectiveParser<>(JavaKeyword.EXPORTS, JavaExportsDirectiveNode::new));
        rules.bind(JavaModuleRoles.OPENS, JavaPackageAccessDirectiveParser.class.getName() + "#" + JavaKeyword.OPENS.name(),
                new JavaPackageAccessDirectiveParser<>(JavaKeyword.OPENS, JavaOpensDirectiveNode::new));
        rules.bind(JavaModuleRoles.USES, JavaUsesDirectiveParser.class.getName(), new JavaUsesDirectiveParser());
        rules.bind(JavaModuleRoles.PROVIDES, JavaProvidesDirectiveParser.class.getName(), new JavaProvidesDirectiveParser());
        rules.bind(JavaModuleRoles.DIRECTIVE, JavaModuleDirectiveParser.class.getName(), new JavaModuleDirectiveParser(Map.of(
                JavaKeyword.REQUIRES, JavaModuleRoles.REQUIRES,
                JavaKeyword.EXPORTS, JavaModuleRoles.EXPORTS,
                JavaKeyword.OPENS, JavaModuleRoles.OPENS,
                JavaKeyword.USES, JavaModuleRoles.USES,
                JavaKeyword.PROVIDES, JavaModuleRoles.PROVIDES)));
    }
}
