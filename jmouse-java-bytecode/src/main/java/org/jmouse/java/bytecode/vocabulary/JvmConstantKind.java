package org.jmouse.java.bytecode.vocabulary;

/** Standard tags and explicitly installed future classfile tags share this contract. */
public interface JvmConstantKind {
    String name();

    int code();

    int slots();
}
