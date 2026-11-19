package org.jmouse.java.bytecode.validation;

import org.jmouse.el.lexer.BytesCursorException;
import org.jmouse.el.node.Node;
import org.jmouse.el.parser.ParseSession;
import org.jmouse.java.bytecode.JavaBytecodeLanguage;
import org.jmouse.java.bytecode.JavaBytecodeMode;
import org.jmouse.java.bytecode.node.JavaBytecodeNode;
import org.jmouse.java.bytecode.node.JavaClassFileNode;
import org.jmouse.java.bytecode.node.attribute.JavaCodeAttributeNode;
import org.jmouse.java.bytecode.node.constant.JavaConstantNode;
import org.jmouse.java.bytecode.signature.JvmDescriptorRoles;
import org.jmouse.java.bytecode.signature.JvmReferenceTypeSignatureNode;
import org.jmouse.java.bytecode.signature.JvmSignatureRoles;
import org.jmouse.java.bytecode.signature.JvmTextGrammar;
import org.jmouse.java.bytecode.vocabulary.JvmClassAccessFlag;
import org.jmouse.java.bytecode.vocabulary.JvmSignatureToken;
import org.jmouse.java.model.descriptor.JavaClassDescriptor;
import org.jmouse.java.model.descriptor.JavaMethodDescriptor;
import org.jmouse.java.model.descriptor.JavaTypeDescriptor;
import org.jmouse.java.model.symbol.JavaJvmNames;

import java.util.HashMap;
import java.util.Map;

/** Invocation-local caches and provenance; no classes are loaded while validating references. */
public final class JvmValidationContext {
    private final JavaClassFileNode root;
    private final JavaBytecodeLanguage language;
    private final JavaBytecodeMode mode;
    private final ParseSession session;
    private final Map<String, JavaTypeDescriptor> fields = new HashMap<>();
    private final Map<String, JvmReferenceTypeSignatureNode> signatures = new HashMap<>();
    private final Map<String, JavaMethodDescriptor> methods = new HashMap<>();
    private final Map<JavaCodeAttributeNode, JvmCodeLayout> layouts = new HashMap<>();

    public JvmValidationContext(
            JavaClassFileNode root,
            JavaBytecodeLanguage language,
            JavaBytecodeMode mode,
            ParseSession session) {
        this.root = root;
        this.language = language;
        this.mode = mode;
        this.session = session;
    }

    public JavaClassFileNode root() {
        return root;
    }

    public JvmCodeLayout codeLayout(JavaBytecodeNode node) {
        JavaCodeAttributeNode code =
                node instanceof JavaCodeAttributeNode value
                        ? value
                        : enclosing(node, JavaCodeAttributeNode.class);
        return layouts.computeIfAbsent(code, value -> new JvmCodeLayout(value, this));
    }

    public static <N extends Node> N enclosing(Node node, Class<N> type) {
        for (Node parent = node.getParent(); parent != null; parent = parent.getParent()) {
            if (type.isInstance(parent)) {
                return type.cast(parent);
            }
        }
        throw new IllegalArgumentException("Missing enclosing " + type.getSimpleName());
    }

    public JavaBytecodeMode mode() {
        return mode;
    }

    public ParseSession session() {
        return session;
    }

    public JavaTypeDescriptor field(String text) {
        return fields.computeIfAbsent(
                text,
                encoding ->
                        JvmTextGrammar.parseEmbedded(
                                        language.textLexer(),
                                        root.source().getName(),
                                        encoding,
                                        JvmDescriptorRoles.FIELD,
                                        session)
                                .descriptor());
    }

    public JvmReferenceTypeSignatureNode referenceSignature(String text) {
        return signatures.computeIfAbsent(
                text,
                encoding -> {
                    JvmReferenceTypeSignatureNode node =
                            JvmTextGrammar.parseEmbedded(
                                    language.textLexer(),
                                    root.source().getName(),
                                    encoding,
                                    JvmSignatureRoles.REFERENCE,
                                    session);
                    node.freeze();
                    return node;
                });
    }

    public JavaMethodDescriptor method(String text) {
        return methods.computeIfAbsent(
                text,
                encoding ->
                        JvmTextGrammar.parseEmbedded(
                                        language.textLexer(),
                                        root.source().getName(),
                                        encoding,
                                        JvmDescriptorRoles.METHOD,
                                        session)
                                .descriptor());
    }

    public <N extends JavaConstantNode> N constant(int index, Class<N> type) {
        return root.pool().require(index, type);
    }

    public String utf8(int index) {
        return root.pool().utf8(index);
    }

    public String className(int index) {
        String name = root.pool().className(index);
        if (!name.isEmpty() && name.charAt(0) == JvmSignatureToken.ARRAY.code()) {
            field(name);
        } else {
            new JavaClassDescriptor(name);
        }
        return name;
    }

    public boolean isInterface() {
        return (root.accessFlags() & JvmClassAccessFlag.INTERFACE.code()) != 0;
    }

    public void unqualifiedName(String name) {
        if (name.isEmpty()
                || name.codePoints()
                        .anyMatch(
                                character ->
                                        character == JvmSignatureToken.INNER_TYPE.code()
                                                || character == JvmSignatureToken.SEMICOLON.code()
                                                || character == JvmSignatureToken.ARRAY.code()
                                                || character
                                                        == JvmSignatureToken.PACKAGE_SEPARATOR
                                                                .code())) {
            throw new IllegalArgumentException("Invalid JVM unqualified name: " + name);
        }
    }

    public void methodName(String name) {
        unqualifiedName(name);
        if (!name.equals(JavaJvmNames.CONSTRUCTOR)
                && !name.equals(JavaJvmNames.CLASS_INITIALIZER)
                && name.codePoints()
                        .anyMatch(
                                character ->
                                        character == JvmSignatureToken.ARGUMENTS_OPEN.code()
                                                || character
                                                        == JvmSignatureToken.ARGUMENTS_CLOSE
                                                                .code())) {
            throw new IllegalArgumentException("Invalid JVM method name: " + name);
        }
    }

    public void require(boolean condition, JavaBytecodeNode node, String message) {
        if (!condition) {
            throw failure(node, message);
        }
    }

    public BytesCursorException failure(JavaBytecodeNode node, String message) {
        return new BytesCursorException(node.source().getName(), node.span().start(), message);
    }

    public void at(JavaBytecodeNode node, Runnable validation) {
        session.step();
        try {
            validation.run();
        } catch (IllegalArgumentException exception) {
            if (exception instanceof BytesCursorException binary) {
                throw binary;
            }
            BytesCursorException failure = failure(node, exception.getMessage());
            failure.initCause(exception);
            throw failure;
        }
    }
}
