package org.jmouse.java.index.binary;

import org.jmouse.java.bytecode.JavaBytecodeMode;
import org.jmouse.java.bytecode.JavaBytecodeParser;
import org.jmouse.java.bytecode.JavaBytecodeResult;
import org.jmouse.java.index.FileIndex;
import org.jmouse.java.model.version.VersionAdmission;

import java.util.Objects;

/** Class bytes are decoded once into the own AST; the API view is never reparsed as Java source. */
public final class ClassFileIndexer {
    public static final String REVISION = "2";

    private final JavaBytecodeParser parser;
    private final ClassFileIndexProjection projection;

    public ClassFileIndexer() {
        this(new JavaBytecodeParser(), new StandardClassFileIndexProjection());
    }

    public ClassFileIndexer(ClassFileProjectionConfiguration configuration) {
        this(new JavaBytecodeParser(), new StandardClassFileIndexProjection(configuration));
    }

    public ClassFileIndexer(JavaBytecodeParser parser, ClassFileIndexProjection projection) {
        this.parser = Objects.requireNonNull(parser, "parser");
        this.projection = Objects.requireNonNull(projection, "projection");
    }

    public IndexedClass index(String source, byte[] classBytes) {
        return index(source, parser.parse(source, classBytes, JavaBytecodeMode.DECLARATIONS_ONLY));
    }

    public IndexedClass index(String source, JavaBytecodeResult classFile) {
        if (classFile.admission() != VersionAdmission.ACCEPTED) {
            throw new IllegalArgumentException(
                    "Binary indexing requires an admitted classfile version");
        }
        return projection.project(source, classFile.root());
    }

    public record IndexedClass(FileIndex file, String text) {}
}
