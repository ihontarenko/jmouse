package org.jmouse.java.parser.java27;

import org.jmouse.el.parser.ParseRole;
import org.jmouse.java.parser.grammar.JavaParsingInput;

public final class Java27Roles {
    public static final ParseRole<JavaParsingInput, Java27InspectNode> INSPECT =
            new ParseRole<>(
                    Java27Names.INSPECT_ROLE, JavaParsingInput.class, Java27InspectNode.class);
    public static final ParseRole<JavaParsingInput, Java27ValueNode> VALUE =
            new ParseRole<>(Java27Names.VALUE_ROLE, JavaParsingInput.class, Java27ValueNode.class);
    public static final ParseRole<JavaParsingInput, Java27BundleImportNode> IMPORT =
            new ParseRole<>(
                    Java27Names.IMPORT_ROLE, JavaParsingInput.class, Java27BundleImportNode.class);

    private Java27Roles() {}
}
