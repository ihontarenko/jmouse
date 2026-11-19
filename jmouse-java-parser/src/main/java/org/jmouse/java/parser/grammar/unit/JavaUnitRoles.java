package org.jmouse.java.parser.grammar.unit;

import org.jmouse.java.parser.grammar.JavaParseRoleNames;

import org.jmouse.el.parser.ParseRole;
import org.jmouse.java.parser.grammar.JavaParsingInput;
import org.jmouse.java.parser.node.unit.JavaCompilationUnitNode;
import org.jmouse.java.parser.node.unit.JavaImportDeclarationNode;
import org.jmouse.java.parser.node.unit.JavaImportNode;
import org.jmouse.java.parser.node.unit.JavaPackageDeclarationNode;

public final class JavaUnitRoles {
    public static final ParseRole<JavaParsingInput, JavaCompilationUnitNode> COMPILATION_UNIT =
            new ParseRole<>(JavaParseRoleNames.COMPILATION_UNIT, JavaParsingInput.class, JavaCompilationUnitNode.class);
    public static final ParseRole<JavaParsingInput, JavaImportNode> IMPORT =
            new ParseRole<>(JavaParseRoleNames.IMPORT, JavaParsingInput.class, JavaImportNode.class);
    public static final ParseRole<JavaParsingInput, JavaPackageDeclarationNode> PACKAGE =
            new ParseRole<>(JavaParseRoleNames.PACKAGE, JavaParsingInput.class, JavaPackageDeclarationNode.class);

    private JavaUnitRoles() {
    }
}
