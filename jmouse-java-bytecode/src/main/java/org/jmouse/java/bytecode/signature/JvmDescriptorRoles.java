package org.jmouse.java.bytecode.signature;

import org.jmouse.el.parser.ParseRole;

public final class JvmDescriptorRoles {
    public static final ParseRole<JvmTextInput, JvmDescriptorTypeNode> FIELD =
            new ParseRole<>(
                    JvmTextNames.FIELD_DESCRIPTOR, JvmTextInput.class, JvmDescriptorTypeNode.class);
    public static final ParseRole<JvmTextInput, JvmDescriptorTypeNode> RETURN =
            new ParseRole<>(
                    JvmTextNames.RETURN_DESCRIPTOR,
                    JvmTextInput.class,
                    JvmDescriptorTypeNode.class);
    public static final ParseRole<JvmTextInput, JvmMethodDescriptorNode> METHOD =
            new ParseRole<>(
                    JvmTextNames.METHOD_DESCRIPTOR,
                    JvmTextInput.class,
                    JvmMethodDescriptorNode.class);

    private JvmDescriptorRoles() {}
}
