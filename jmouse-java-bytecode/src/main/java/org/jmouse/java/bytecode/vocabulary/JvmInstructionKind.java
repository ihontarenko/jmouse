package org.jmouse.java.bytecode.vocabulary;

/** Standard opcode identities and explicitly contributed future instruction identities. */
public interface JvmInstructionKind {
    String name();

    int code();

    String mnemonic();
}
