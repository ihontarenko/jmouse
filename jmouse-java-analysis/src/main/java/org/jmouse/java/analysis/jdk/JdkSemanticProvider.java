package org.jmouse.java.analysis.jdk;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Trees;

import org.jmouse.java.analysis.JavaAnalysisDiagnosticCodes;
import org.jmouse.java.analysis.JavaAnalysisRequest;
import org.jmouse.java.analysis.JavaAnalysisResult;
import org.jmouse.java.analysis.JavaAnalyzedProgram;
import org.jmouse.java.analysis.JavaDiagnosticSeverity;
import org.jmouse.java.analysis.JavaSemanticCoverage;
import org.jmouse.java.analysis.JavaSemanticDiagnostic;
import org.jmouse.java.analysis.JavaSemanticProvider;
import org.jmouse.java.model.symbol.JavaSourceLocation;
import org.jmouse.java.model.version.PreviewPolicy;
import org.jmouse.java.parser.JavaSyntaxResult;
import org.jmouse.java.parser.grammar.JavaParsingMode;
import org.jmouse.java.parser.node.JavaSyntaxNode;
import org.jmouse.java.parser.node.unit.JavaCompilationUnitNode;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

/**
 * Full-unit JDK attribution behind the provider seam. Native compiler objects never escape this
 * invocation.
 */
public final class JdkSemanticProvider implements JavaSemanticProvider {
    @Override
    public JavaAnalysisResult analyze(JavaAnalysisRequest request) {
        JdkSourceIndex source = new JdkSourceIndex();
        List<JdkSourceFile> files = new ArrayList<>();
        List<JavaSemanticDiagnostic> diagnostics = new ArrayList<>();
        for (JavaSyntaxResult<JavaCompilationUnitNode> syntax : request.units()) {
            JdkSourceFile file = new JdkSourceFile(syntax);
            files.add(file);
            source.add(file, syntax);
            if (!syntax.isComplete() || syntax.mode() != JavaParsingMode.COMPLETE) {
                diagnostics.add(
                        error(
                                JavaAnalysisDiagnosticCodes.INCOMPLETE_SYNTAX,
                                "Semantic analysis requires a complete syntax tree",
                                Optional.of(
                                        new JavaSourceLocation(
                                                syntax.source().name(),
                                                syntax.root().span().start().offset(),
                                                syntax.root().span().end().offset()))));
            }
        }
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            diagnostics.add(
                    error(
                            JavaAnalysisDiagnosticCodes.BACKEND_UNAVAILABLE,
                            "The selected semantic provider requires a JDK compiler",
                            Optional.empty()));
        }
        if (!diagnostics.isEmpty()) {
            return failure(source, diagnostics);
        }
        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        try (StandardJavaFileManager manager =
                compiler.getStandardFileManager(collector, Locale.ROOT, StandardCharsets.UTF_8)) {
            manager.setLocationFromPaths(StandardLocation.CLASS_PATH, request.classPath());
            manager.setLocationFromPaths(StandardLocation.MODULE_PATH, request.modulePath());
            manager.setLocationFromPaths(StandardLocation.SOURCE_PATH, List.of());
            List<String> options =
                    new ArrayList<>(
                            List.of(
                                    JdkCompilerOptions.RELEASE,
                                    Integer.toString(request.profile().release().number()),
                                    JdkCompilerOptions.NO_PROCESSORS,
                                    JdkCompilerOptions.NO_IMPLICIT_OUTPUT));
            if (request.profile().preview() instanceof PreviewPolicy.EnabledForRelease) {
                options.add(JdkCompilerOptions.ENABLE_PREVIEW);
            }
            JavacTask task;
            try {
                task =
                        (JavacTask)
                                compiler.getTask(
                                        new StringWriter(),
                                        new JdkReadOnlyFileManager(manager),
                                        collector,
                                        options,
                                        null,
                                        files);
            } catch (IllegalArgumentException exception) {
                diagnostics.add(
                        error(
                                JavaAnalysisDiagnosticCodes.UNSUPPORTED_PROFILE,
                                exception.toString(),
                                Optional.empty()));
                return failure(source, diagnostics);
            }
            List<CompilationUnitTree> trees = new ArrayList<>();
            task.parse().forEach(trees::add);
            task.analyze();
            Trees nativeTrees = Trees.instance(task);
            JdkSymbolMapping symbols =
                    new JdkSymbolMapping(task.getElements(), task.getTypes(), nativeTrees, source);
            for (CompilationUnitTree unit : trees) {
                new JdkSemanticScanner(
                                unit,
                                nativeTrees,
                                task.getTypes(),
                                task.getElements(),
                                source,
                                symbols)
                        .scan(unit, null);
            }
            collector
                    .getDiagnostics()
                    .forEach(diagnostic -> diagnostics.add(diagnostic(diagnostic, files, request)));
            JavaSemanticCoverage coverage = JavaSemanticCoverage.COMPLETE;
            for (JavaSyntaxNode missing : JdkCoverageMapping.missing(source)) {
                coverage = JavaSemanticCoverage.PARTIAL;
                if (diagnostics.stream()
                        .noneMatch(problem -> problem.severity() == JavaDiagnosticSeverity.ERROR)) {
                    diagnostics.add(
                            error(
                                    JavaAnalysisDiagnosticCodes.UNMAPPED_OPERATION,
                                    "Incomplete semantic facts for "
                                            + missing.schema().kind().name(),
                                    Optional.of(
                                            new JavaSourceLocation(
                                                    missing.sourceName(),
                                                    missing.span().start().offset(),
                                                    missing.span().end().offset()))));
                }
            }
            return new JavaAnalysisResult(
                    new JavaAnalyzedProgram(
                            source.identities,
                            source.facts,
                            symbols.symbols,
                            symbols.upperBounds,
                            source.scopes,
                            symbols.executableAbis,
                            source.capturedValues,
                            symbols.constants,
                            List.copyOf(symbols.overrides),
                            source.functionalTargets,
                            source.intrinsics,
                            coverage),
                    diagnostics);
        } catch (IOException | RuntimeException exception) {
            diagnostics.add(
                    error(
                            JavaAnalysisDiagnosticCodes.ANALYSIS_FAILURE,
                            exception.toString(),
                            Optional.empty()));
        }
        collector
                .getDiagnostics()
                .forEach(diagnostic -> diagnostics.add(diagnostic(diagnostic, files, request)));
        return failure(source, diagnostics);
    }

    private static JavaSemanticDiagnostic diagnostic(
            Diagnostic<? extends JavaFileObject> diagnostic,
            List<JdkSourceFile> files,
            JavaAnalysisRequest request) {
        JavaDiagnosticSeverity severity =
                diagnostic.getKind() == Diagnostic.Kind.ERROR
                        ? JavaDiagnosticSeverity.ERROR
                        : diagnostic.getKind() == Diagnostic.Kind.WARNING
                                        || diagnostic.getKind() == Diagnostic.Kind.MANDATORY_WARNING
                                ? JavaDiagnosticSeverity.WARNING
                                : JavaDiagnosticSeverity.NOTE;
        Optional<JavaSourceLocation> location = Optional.empty();
        if (diagnostic.getSource() != null) {
            for (int index = 0; index < files.size(); index++) {
                if (files.get(index).toUri().equals(diagnostic.getSource().toUri())) {
                    long start =
                            diagnostic.getStartPosition() < 0
                                    ? diagnostic.getPosition()
                                    : diagnostic.getStartPosition();
                    long end =
                            diagnostic.getEndPosition() < start
                                    ? start
                                    : diagnostic.getEndPosition();
                    if (start >= 0 && end <= request.units().get(index).source().text().length()) {
                        location =
                                Optional.of(
                                        new JavaSourceLocation(
                                                request.units().get(index).source().name(),
                                                (int) start,
                                                (int) end));
                    }
                    break;
                }
            }
        }
        return new JavaSemanticDiagnostic(
                severity, diagnostic.getCode(), diagnostic.getMessage(Locale.ROOT), location);
    }

    private static JavaSemanticDiagnostic error(
            String code, String message, Optional<JavaSourceLocation> source) {
        return new JavaSemanticDiagnostic(JavaDiagnosticSeverity.ERROR, code, message, source);
    }

    private static JavaAnalysisResult failure(
            JdkSourceIndex source, List<JavaSemanticDiagnostic> diagnostics) {
        return new JavaAnalysisResult(
                new JavaAnalyzedProgram(
                        source.identities,
                        source.facts,
                        Map.of(),
                        Map.of(),
                        source.scopes,
                        Map.of(),
                        source.capturedValues,
                        Map.of(),
                        List.of(),
                        source.functionalTargets,
                        source.intrinsics,
                        JavaSemanticCoverage.PARTIAL),
                diagnostics);
    }
}
