package org.jmouse.java.parser.projection;

import org.jmouse.el.translate.NodeOperationRegistry;
import org.jmouse.java.parser.lexer.JavaLexicalSnapshot;
import org.jmouse.java.parser.model.Declaration;
import org.jmouse.java.parser.model.Expression;
import org.jmouse.java.parser.model.ImportDeclaration;
import org.jmouse.java.parser.model.TypeDeclaration;

import java.util.List;

/** A frozen set of exact projection bindings, shared safely by independent parsing sessions. */
public final class JavaProjectionConfiguration {
    private final NodeOperationRegistry<JavaProjectionContext, Expression> expressions;
    private final NodeOperationRegistry<JavaLexicalSnapshot, ImportDeclaration> imports;
    private final NodeOperationRegistry<JavaTypeProjectionContext, TypeDeclaration> types;
    private final NodeOperationRegistry<JavaMemberProjectionContext, List<Declaration>> members;
    private final NodeOperationRegistry<JavaStatementProjectionContext, JavaStatementProjection>
            statements;

    private JavaProjectionConfiguration(Builder builder) {
        expressions = builder.expressions.snapshot();
        imports = builder.imports.snapshot();
        types = builder.types.snapshot();
        members = builder.members.snapshot();
        statements = builder.statements.snapshot();
    }

    public static JavaProjectionConfiguration standard() {
        return Standard.INSTANCE;
    }

    public static Builder builder() {
        return standard().toBuilder();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public NodeOperationRegistry<JavaProjectionContext, Expression> expressions() {
        return expressions;
    }

    public NodeOperationRegistry<JavaLexicalSnapshot, ImportDeclaration> imports() {
        return imports;
    }

    public NodeOperationRegistry<JavaTypeProjectionContext, TypeDeclaration> types() {
        return types;
    }

    public NodeOperationRegistry<JavaMemberProjectionContext, List<Declaration>> members() {
        return members;
    }

    public NodeOperationRegistry<JavaStatementProjectionContext, JavaStatementProjection>
            statements() {
        return statements;
    }

    public static final class Builder {
        private final NodeOperationRegistry<JavaProjectionContext, Expression> expressions;
        private final NodeOperationRegistry<JavaLexicalSnapshot, ImportDeclaration> imports;
        private final NodeOperationRegistry<JavaTypeProjectionContext, TypeDeclaration> types;
        private final NodeOperationRegistry<JavaMemberProjectionContext, List<Declaration>> members;
        private final NodeOperationRegistry<JavaStatementProjectionContext, JavaStatementProjection>
                statements;

        private Builder() {
            expressions = new NodeOperationRegistry<>(JavaProjectionNames.EXPRESSION);
            imports = new NodeOperationRegistry<>(JavaProjectionNames.IMPORT);
            types = new NodeOperationRegistry<>(JavaProjectionNames.TYPE);
            members = new NodeOperationRegistry<>(JavaProjectionNames.MEMBER);
            statements = new NodeOperationRegistry<>(JavaProjectionNames.STATEMENT);
        }

        private Builder(JavaProjectionConfiguration previous) {
            expressions = previous.expressions.fork();
            imports = previous.imports.fork();
            types = previous.types.fork();
            members = previous.members.fork();
            statements = previous.statements.fork();
        }

        public NodeOperationRegistry<JavaProjectionContext, Expression> expressions() {
            return expressions;
        }

        public NodeOperationRegistry<JavaLexicalSnapshot, ImportDeclaration> imports() {
            return imports;
        }

        public NodeOperationRegistry<JavaTypeProjectionContext, TypeDeclaration> types() {
            return types;
        }

        public NodeOperationRegistry<JavaMemberProjectionContext, List<Declaration>> members() {
            return members;
        }

        public NodeOperationRegistry<JavaStatementProjectionContext, JavaStatementProjection>
                statements() {
            return statements;
        }

        public JavaProjectionConfiguration build() {
            return new JavaProjectionConfiguration(this);
        }
    }

    private static final class Standard {
        private static final JavaProjectionConfiguration INSTANCE = create();

        private static JavaProjectionConfiguration create() {
            Builder builder = new Builder();
            JavaExpressionProjection.install(builder.expressions);
            JavaDeclarationProjection.install(builder.imports);
            JavaTypeProjection.install(builder.types);
            JavaMemberProjection.install(builder.members);
            JavaStandardStatementProjection.install(builder.statements);
            return builder.build();
        }
    }
}
