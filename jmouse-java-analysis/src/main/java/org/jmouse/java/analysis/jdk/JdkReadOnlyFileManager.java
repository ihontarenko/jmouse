package org.jmouse.java.analysis.jdk;

import java.io.IOException;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;

final class JdkReadOnlyFileManager extends ForwardingJavaFileManager<JavaFileManager> {
    JdkReadOnlyFileManager(JavaFileManager delegate) {
        super(delegate);
    }

    @Override
    public JavaFileObject getJavaFileForOutput(
            Location location, String className, JavaFileObject.Kind kind, FileObject sibling)
            throws IOException {
        throw new IOException("Semantic analysis cannot write class files");
    }

    @Override
    public FileObject getFileForOutput(
            Location location, String packageName, String relativeName, FileObject sibling)
            throws IOException {
        throw new IOException("Semantic analysis cannot write generated files");
    }
}
