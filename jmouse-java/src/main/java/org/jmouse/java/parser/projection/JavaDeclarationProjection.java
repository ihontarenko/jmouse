package org.jmouse.java.parser.projection;

import org.jmouse.el.lexer.TokenizableSource.Entry;
import org.jmouse.el.node.Node;
import org.jmouse.el.translate.Bindings;
import org.jmouse.el.translate.Capabilities;
import org.jmouse.el.translate.TranslationRefusedException;
import org.jmouse.el.translate.Translator;
import org.jmouse.java.parser.JavaSyntaxResult;
import org.jmouse.java.parser.lexer.JavaKeyword;
import org.jmouse.java.parser.lexer.JavaLexicalSnapshot;
import org.jmouse.java.parser.model.DeclarationKind;
import org.jmouse.java.parser.model.ImportDeclaration;
import org.jmouse.java.parser.model.JavaSourceFile;
import org.jmouse.java.parser.model.ModuleDeclaration;
import org.jmouse.java.parser.model.PackageDeclaration;
import org.jmouse.java.parser.model.SymbolId;
import org.jmouse.java.parser.model.TypeDeclaration;
import org.jmouse.java.parser.node.declaration.JavaTypeDeclarationNode;
import org.jmouse.java.parser.node.module.JavaModuleDeclarationNode;
import org.jmouse.java.parser.node.module.JavaModuleKind;
import org.jmouse.java.parser.node.unit.JavaCompilationUnitNode;
import org.jmouse.java.parser.node.unit.JavaImportDeclarationNode;
import org.jmouse.java.parser.node.unit.JavaImportKind;
import org.jmouse.java.parser.node.unit.JavaImportNode;
import org.jmouse.java.parser.node.unit.JavaPackageDeclarationNode;
import org.jmouse.java.parser.source.JavaSourceCoverage;
import org.jmouse.java.parser.source.ParseProblem;
import org.jmouse.java.parser.source.SourceSpan;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Declaration records are a translator view of the full AST, never a second source grammar. */
public final class JavaDeclarationProjection implements Translator<JavaSourceFile> {
    private static final Capabilities CAPABILITIES =
            Capabilities.of(
                    JavaDeclarationProjection.class.getName(),
                    JavaProjectionCapabilities.DECLARATION_METADATA);
    private final JavaLexicalSnapshot source;
    private final List<ParseProblem> problems;
    private final JavaSourceCoverage coverage;

    public JavaDeclarationProjection(JavaLexicalSnapshot source) {
        this(source, source.problems());
    }

    public JavaDeclarationProjection(JavaLexicalSnapshot source, List<ParseProblem> problems) {
        this(source, problems, JavaSourceCoverage.unspecified());
    }

    public JavaDeclarationProjection(
            JavaLexicalSnapshot source, List<ParseProblem> problems, JavaSourceCoverage coverage) {
        this.source = Objects.requireNonNull(source, "source");
        this.problems = List.copyOf(problems);
        this.coverage = Objects.requireNonNull(coverage, "coverage");
        if (!this.problems.containsAll(source.problems())) {
            throw new IllegalArgumentException(
                    "A declaration view must preserve lexical diagnostics");
        }
    }

    public static JavaSourceFile project(JavaSyntaxResult<JavaCompilationUnitNode> syntax) {
        return new JavaDeclarationProjection(syntax.source(), syntax.problems(), syntax.coverage())
                .translate(syntax.root());
    }

    @Override
    public Capabilities capabilities() {
        return CAPABILITIES;
    }

    @Override
    public JavaSourceFile translate(Node root, Bindings bindings) {
        if (!(root instanceof JavaCompilationUnitNode unit) || !bindings.isEmpty()) {
            throw new TranslationRefusedException(
                    "Declaration metadata requires a compilation unit and no runtime bindings");
        }
        JavaProjectionContext context = new JavaProjectionContext(source, unit);
        return translate(unit, context);
    }

    JavaSourceFile translate(JavaCompilationUnitNode unit, JavaProjectionContext context) {
        PackageDeclaration packageDeclaration =
                unit.packageDeclaration()
                        .map(node -> packageDeclaration(context, node))
                        .orElse(null);
        String packageName = packageDeclaration == null ? "" : packageDeclaration.name();
        List<ImportDeclaration> imports =
                unit.imports().stream().map(JavaDeclarationProjection::importDeclaration).toList();
        ModuleDeclaration moduleDeclaration =
                unit.module().map(node -> moduleDeclaration(context, node)).orElse(null);
        List<TypeDeclaration> types = new ArrayList<>();
        for (Node declaration : unit.declarations()) {
            if (declaration instanceof JavaTypeDeclarationNode type) {
                types.add(JavaTypeProjection.project(context, type, packageName));
            }
        }
        return new JavaSourceFile(
                source.name(),
                packageDeclaration,
                moduleDeclaration,
                imports,
                types,
                source.comments(),
                context.documentation.documents(),
                problems,
                context.references.references(),
                List.of(),
                List.of(),
                coverage);
    }

    private static PackageDeclaration packageDeclaration(
            JavaProjectionContext context, JavaPackageDeclarationNode node) {
        String name = JavaSourceSpelling.name(node.name());
        return new PackageDeclaration(
                name,
                context.annotations(node.annotations()),
                node.span(),
                context.document(
                        node,
                        new SymbolId(name),
                        DeclarationKind.PACKAGE,
                        name,
                        JavaKeyword.PACKAGE.spelling() + " " + name));
    }

    private static ImportDeclaration importDeclaration(JavaImportNode syntax) {
        if (!(syntax instanceof JavaImportDeclarationNode node)) {
            throw new TranslationRefusedException(
                    "The declaration view does not support import "
                            + syntax.schema().kind().name());
        }
        boolean isStatic =
                node.kind() == JavaImportKind.SINGLE_STATIC
                        || node.kind() == JavaImportKind.STATIC_ON_DEMAND;
        boolean onDemand =
                node.kind() == JavaImportKind.TYPE_ON_DEMAND
                        || node.kind() == JavaImportKind.STATIC_ON_DEMAND;
        return new ImportDeclaration(
                JavaSourceSpelling.name(node.name()),
                isStatic,
                onDemand,
                node.name().span(),
                node.span());
    }

    private static ModuleDeclaration moduleDeclaration(
            JavaProjectionContext context, JavaModuleDeclarationNode node) {
        String name = JavaSourceSpelling.name(node.name());
        Entry opening =
                context.source
                        .tokens()
                        .get(context.source.tokenIndexAtOrAfter(node.name().span().end().offset()));
        SourceSpan body = context.source.span(opening.offset(), node.span().end().offset());
        SourceSpan header = context.header(node, opening.offset());
        return new ModuleDeclaration(
                name,
                node.kind() == JavaModuleKind.OPEN,
                context.annotations(node.annotations()),
                body,
                node.span(),
                context.document(
                        node,
                        new SymbolId(name),
                        DeclarationKind.MODULE,
                        name,
                        context.spelling.collapse(header)));
    }
}
