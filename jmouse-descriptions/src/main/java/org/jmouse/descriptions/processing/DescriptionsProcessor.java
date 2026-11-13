package org.jmouse.descriptions.processing;

import org.jmouse.descriptions.Descriptions;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

/**
 * 📝 Keeps the Javadoc of every record and enum it compiles beside the class, as {@link Descriptions} reads it back: a record's
 * own words and each component's {@code @param}, an enum's own words and each constant's comment. It runs in every compilation
 * that has this module on its class path, claims no annotation and changes no class; a type whose Javadoc says nothing gets
 * no file.
 */
@SupportedAnnotationTypes("*")
public final class DescriptionsProcessor extends AbstractProcessor {

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment round) {
        for (Element root : round.getRootElements()) {
            describe(root);
        }
        return false;
    }

    /** A type and every type declared inside it. */
    private void describe(Element element) {
        if (!element.getKind().isClass() && !element.getKind().isInterface()) {
            return;
        }
        if (element.getKind() == ElementKind.RECORD || element.getKind() == ElementKind.ENUM) {
            keep((TypeElement) element);
        }
        for (Element inside : element.getEnclosedElements()) {
            describe(inside);
        }
    }

    private void keep(TypeElement type) {
        JavadocWords words = JavadocWords.of(processingEnv.getElementUtils().getDocComment(type));
        Map<String, String> kept = new TreeMap<>();
        if (!words.summary().isBlank()) {
            kept.put(Descriptions.SUMMARY, words.summary());
        }
        if (type.getKind() == ElementKind.RECORD) {
            for (Element component : type.getRecordComponents()) {
                String said = words.parameters().get(component.getSimpleName().toString());
                if (said != null && !said.isBlank()) {
                    kept.put(Descriptions.MEMBER + component.getSimpleName(), said);
                }
            }
        } else {
            for (Element constant : type.getEnclosedElements()) {
                if (constant.getKind() == ElementKind.ENUM_CONSTANT) {
                    String said = JavadocWords.of(processingEnv.getElementUtils().getDocComment(constant)).summary();
                    if (!said.isBlank()) {
                        kept.put(Descriptions.MEMBER + constant.getSimpleName(), said);
                    }
                }
            }
        }
        if (kept.isEmpty()) {
            return;
        }
        String binaryName = processingEnv.getElementUtils().getBinaryName(type).toString();
        try {
            FileObject file = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "",
                    Descriptions.resourceOf(binaryName), type);
            try (Writer writer = new OutputStreamWriter(file.openOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(propertiesText(kept));
            }
        } catch (IOException unwritable) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "The description of " + binaryName + " cannot be kept: "
                                                                            + unwritable.getMessage(), type);
        }
    }

    /** The words as a properties file, without the date {@link Properties#store} heads it with: a build writes the same bytes twice. */
    private static String propertiesText(Map<String, String> kept) throws IOException {
        Properties properties = new Properties();
        properties.putAll(kept);
        StringWriter written = new StringWriter();
        properties.store(written, null);
        StringBuilder text = new StringBuilder();
        written.toString().lines().filter(line -> !line.startsWith("#")).sorted().forEach(line -> text.append(line).append('\n'));
        return text.toString();
    }
}
