package org.jmouse.java.analysis.jdk;

import org.jmouse.java.parser.JavaSyntaxResult;
import org.jmouse.java.parser.node.unit.JavaCompilationUnitNode;

import java.net.URI;
import java.net.URISyntaxException;

import javax.tools.SimpleJavaFileObject;

final class JdkSourceFile extends SimpleJavaFileObject {
    private final JavaSyntaxResult<JavaCompilationUnitNode> syntax;

    JdkSourceFile(JavaSyntaxResult<JavaCompilationUnitNode> syntax) {
        super(uri(syntax.source().name()), Kind.SOURCE);
        this.syntax = syntax;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
        return syntax.source().text();
    }

    private static URI uri(String name) {
        try {
            return new URI("jmouse", null, "/" + name.replace('\\', '/'), null);
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Invalid source identity", exception);
        }
    }
}
