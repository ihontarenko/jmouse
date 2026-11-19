package org.jmouse.java.parser.grammar.module;

import org.jmouse.java.parser.grammar.JavaParseRoleNames;

import org.jmouse.el.parser.ParseRole;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.node.module.JavaExportsDirectiveNode;
import org.jmouse.java.parser.node.module.JavaModuleDeclarationNode;
import org.jmouse.java.parser.node.module.JavaModuleDirectiveNode;
import org.jmouse.java.parser.node.module.JavaOpensDirectiveNode;
import org.jmouse.java.parser.node.module.JavaProvidesDirectiveNode;
import org.jmouse.java.parser.node.module.JavaRequiresDirectiveNode;
import org.jmouse.java.parser.node.module.JavaUsesDirectiveNode;
import org.jmouse.java.parser.node.name.JavaModifiersNode;

public final class JavaModuleRoles {
    public static final ParseRole<JavaParsingInput, JavaModuleDeclarationNode> MODULE =
            new ParseRole<>(JavaParseRoleNames.MODULE_DECLARATION, JavaParsingInput.class, JavaModuleDeclarationNode.class);
    public static final ParseRole<JavaParsingInput, JavaModuleDirectiveNode> DIRECTIVE =
            new ParseRole<>(JavaParseRoleNames.MODULE_DIRECTIVE, JavaParsingInput.class, JavaModuleDirectiveNode.class);
    public static final ParseRole<JavaParsingInput, JavaRequiresDirectiveNode> REQUIRES =
            new ParseRole<>(JavaParseRoleNames.MODULE_REQUIRES, JavaParsingInput.class, JavaRequiresDirectiveNode.class);
    public static final ParseRole<JavaParsingInput, JavaExportsDirectiveNode> EXPORTS =
            new ParseRole<>(JavaParseRoleNames.MODULE_EXPORTS, JavaParsingInput.class, JavaExportsDirectiveNode.class);
    public static final ParseRole<JavaParsingInput, JavaOpensDirectiveNode> OPENS =
            new ParseRole<>(JavaParseRoleNames.MODULE_OPENS, JavaParsingInput.class, JavaOpensDirectiveNode.class);
    public static final ParseRole<JavaParsingInput, JavaUsesDirectiveNode> USES =
            new ParseRole<>(JavaParseRoleNames.MODULE_USES, JavaParsingInput.class, JavaUsesDirectiveNode.class);
    public static final ParseRole<JavaParsingInput, JavaProvidesDirectiveNode> PROVIDES =
            new ParseRole<>(JavaParseRoleNames.MODULE_PROVIDES, JavaParsingInput.class, JavaProvidesDirectiveNode.class);
    public static final ParseRole<JavaParsingInput, JavaModifiersNode> REQUIRES_MODIFIERS =
            new ParseRole<>(JavaParseRoleNames.MODULE_REQUIRES_MODIFIERS, JavaParsingInput.class, JavaModifiersNode.class);

    private JavaModuleRoles() {
    }
}
