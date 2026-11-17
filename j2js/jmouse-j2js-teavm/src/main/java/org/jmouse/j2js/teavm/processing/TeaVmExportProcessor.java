package org.jmouse.j2js.teavm.processing;

import org.jmouse.j2js.BoundaryValue;
import org.jmouse.j2js.Export;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 🌐 The TeaVM binding of {@link Export}: while a class compiles, every one of its exported methods is checked against the
 * {@link BoundaryValue}s, and the class gets a module class beside it — {@code <Class>Exports} — whose methods carry
 * TeaVM's {@code @JSExport} and call the exported ones. That module class is what TeaVM compiles ({@code mainClass}), so
 * the class itself names no compiler.
 *
 * <p>Beside it go the module's TypeScript declarations, each export typed as {@link BoundaryValue#typeScriptType()} says and
 * documented by its Javadoc: {@code j2js/<module>.d.ts} among the compiled classes, {@code <module>} the compilation's
 * {@value #MODULE_OPTION} option — the name the module's script is written under —, else the module class's name. A page's
 * TypeScript is typed by the Java it calls, never by a copy of it.</p>
 *
 * <p>⚠️ A method that cannot be exported is an error of the compilation, with the method named: a page must never be the
 * first to find out.</p>
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
@SupportedAnnotationTypes("org.jmouse.j2js.Export")
@SupportedOptions(TeaVmExportProcessor.MODULE_OPTION)
public final class TeaVmExportProcessor extends AbstractProcessor {

    /** The option naming the module: its script is {@code <module>.js}, its declarations {@code <module>.d.ts}. */
    public static final String MODULE_OPTION = "j2js.module";

    /** Where among the compiled classes a module's declarations are written. */
    public static final String DECLARATIONS_DIRECTORY = "j2js";

    /** What a module class is called: its class's name and this. */
    public static final String MODULE_SUFFIX = "Exports";

    private static final char LINE_BREAK = '\n';

    /** The package a source names its types of without importing them. */
    private static final String JAVA_LANG = "java.lang.";

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
        Map<TypeElement, List<ExecutableElement>> exportedByClass = new LinkedHashMap<>();

        for (Element element : round.getElementsAnnotatedWith(Export.class)) {
            ExecutableElement method = (ExecutableElement) element;
            if (isExportable(method)) {
                exportedByClass.computeIfAbsent((TypeElement) method.getEnclosingElement(), owner -> new ArrayList<>())
                        .add(method);
            }
        }

        exportedByClass.forEach(this::writeModule);

        return true;
    }

    /** Whether the method may be exported; says why not, on the method, where it may not. */
    private boolean isExportable(ExecutableElement method) {
        if (!method.getModifiers().containsAll(List.of(Modifier.PUBLIC, Modifier.STATIC))) {
            refuse(method, "an exported method is public static: a page calls it on no object");
            return false;
        }

        if (method.getEnclosingElement().getKind() != ElementKind.CLASS) {
            refuse(method, "an exported method belongs to a class");
            return false;
        }

        boolean crosses = crossesAt(method, method.getReturnType(), BoundaryValue.Position.RESULT, "its result");

        for (VariableElement parameter : method.getParameters()) {
            crosses &= crossesAt(method, parameter.asType(), BoundaryValue.Position.PARAMETER,
                                 "parameter '" + parameter.getSimpleName() + "'");
        }

        return crosses;
    }

    private boolean crossesAt(ExecutableElement method, TypeMirror type, BoundaryValue.Position position, String what) {
        String  javaType = javaTypeName(type);
        boolean crosses  = BoundaryValue.ofJavaType(javaType).map(value -> value.crossesAt(position)).orElse(false);

        if (!crosses) {
            refuse(method, "%s is %s, which does not cross to a page — numbers, truth values, text and arrays of them do; anything structured crosses as JSON text"
                    .formatted(what, javaType));
        }

        return crosses;
    }

    private void refuse(ExecutableElement method, String why) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "@Export: " + why, method);
    }

    /** A type as the boundary names it: its qualified name, no type annotation, an array by its component. */
    private static String javaTypeName(TypeMirror type) {
        return switch (type.getKind()) {
            case ARRAY -> javaTypeName(((ArrayType) type).getComponentType()) + "[]";
            case DECLARED -> ((TypeElement) ((DeclaredType) type).asElement()).getQualifiedName().toString();
            case BOOLEAN, BYTE, SHORT, INT, LONG, CHAR, FLOAT, DOUBLE, VOID -> type.getKind().name().toLowerCase(Locale.ROOT);
            default -> type.toString();
        };
    }

    /** A boundary type as the module class writes it: {@code java.lang} types by their simple name, as in any source. */
    private static String sourceName(TypeMirror type) {
        String name = javaTypeName(type);
        return name.startsWith(JAVA_LANG) ? name.substring(JAVA_LANG.length()) : name;
    }

    /** The module class of {@code owner}: one {@code @JSExport} method calling each exported one, and the entry TeaVM wants. */
    private void writeModule(TypeElement owner, List<ExecutableElement> methods) {
        String ownerName   = owner.getQualifiedName().toString();
        String packageName = processingEnv.getElementUtils().getPackageOf(owner).getQualifiedName().toString();
        String moduleName  = owner.getSimpleName() + MODULE_SUFFIX;

        StringBuilder source = new StringBuilder();

        if (!packageName.isEmpty()) {
            source.append("package ").append(packageName).append(";\n\n");
        }

        source.append("""
                import javax.annotation.processing.Generated;
                import org.teavm.jso.JSExport;

                /**
                 * What a page may call of {@link %1$s}: written by the TeaVM binding of jmouse-j2js from its @Export methods.
                 * TeaVM compiles this class into the module.
                 */
                @Generated("%2$s")
                public final class %3$s {

                    private %3$s() {
                    }
                """.formatted(owner.getSimpleName(), TeaVmExportProcessor.class.getName(), moduleName));

        for (ExecutableElement method : methods) {
            source.append(moduleMethod(owner.getSimpleName().toString(), method));
        }

        source.append("""

                    /** TeaVM's entry: nothing runs when the module loads — the page calls the exports. */
                    public static void main(String[] arguments) {
                    }
                }
                """);

        try (Writer writer = processingEnv.getFiler().createSourceFile(packageName.isEmpty() ? moduleName : packageName + "." + moduleName, owner).openWriter()) {
            writer.write(source.toString());
        } catch (IOException exception) {
            throw new UncheckedIOException("The module class of " + ownerName + " could not be written", exception);
        }

        writeDeclarations(owner, moduleName, methods);
    }

    /** The module's TypeScript declarations: a function for each export, its Javadoc its documentation. */
    private void writeDeclarations(TypeElement owner, String moduleClass, List<ExecutableElement> methods) {
        String        module       = processingEnv.getOptions().getOrDefault(MODULE_OPTION, moduleClass);
        StringBuilder declarations = new StringBuilder("""
                // What a page may call of %s: written by the TeaVM binding of jmouse-j2js from its @Export methods.
                """.formatted(owner.getQualifiedName()));

        for (ExecutableElement method : methods) {
            String documentation = processingEnv.getElementUtils().getDocComment(method);
            String parameters    = method.getParameters().stream()
                    .map(parameter -> parameter.getSimpleName() + ": " + typeScriptType(parameter.asType()))
                    .collect(Collectors.joining(", "));

            declarations.append(LINE_BREAK);
            if (documentation != null && !documentation.isBlank()) {
                declarations.append("/**").append(LINE_BREAK);
                documentation.strip().lines().map(String::strip)
                        .forEach(line -> declarations.append(line.isEmpty() ? " *" : " * " + line).append(LINE_BREAK));
                declarations.append(" */").append(LINE_BREAK);
            }
            declarations.append("export function %s(%s): %s;".formatted(method.getSimpleName(), parameters, typeScriptType(method.getReturnType())))
                    .append(LINE_BREAK);
        }

        try (Writer writer = processingEnv.getFiler()
                .createResource(StandardLocation.CLASS_OUTPUT, "", DECLARATIONS_DIRECTORY + "/" + module + ".d.ts", owner).openWriter()) {
            writer.write(declarations.toString());
        } catch (IOException exception) {
            throw new UncheckedIOException("The declarations of " + owner.getQualifiedName() + " could not be written", exception);
        }
    }

    /** The TypeScript type of a type that crosses: every one that reaches here does, for the method was checked before. */
    private static String typeScriptType(TypeMirror type) {
        return BoundaryValue.ofJavaType(javaTypeName(type)).orElseThrow().typeScriptType();
    }

    private static String moduleMethod(String ownerName, ExecutableElement method) {
        String result     = sourceName(method.getReturnType());
        String name       = method.getSimpleName().toString();
        String parameters = method.getParameters().stream()
                .map(parameter -> sourceName(parameter.asType()) + " " + parameter.getSimpleName())
                .collect(Collectors.joining(", "));
        String arguments  = method.getParameters().stream()
                .map(parameter -> parameter.getSimpleName().toString())
                .collect(Collectors.joining(", "));
        String call       = "%s.%s(%s);".formatted(ownerName, name, arguments);

        return """

                    @JSExport
                    public static %s %s(%s) {
                        %s%s
                    }
                """.formatted(result, name, parameters, "void".equals(result) ? "" : "return ", call);
    }
}
