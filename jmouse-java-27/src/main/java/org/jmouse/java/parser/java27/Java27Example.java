package org.jmouse.java.parser.java27;

import org.jmouse.java.model.version.ClassFileVersion;
import org.jmouse.java.model.version.JavaFeatureDefinition;
import org.jmouse.java.model.version.JavaFeatureId;
import org.jmouse.java.model.version.JavaRelease;
import org.jmouse.java.model.version.JavaSourceProfile;
import org.jmouse.java.model.version.JavaSpecificationCatalog;
import org.jmouse.java.model.version.PreviewPolicy;
import org.jmouse.java.parser.JavaParser;
import org.jmouse.java.parser.JavaSyntaxParser;
import org.jmouse.java.parser.extension.JavaSourceLanguage;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Executable extension example. Its invented constructs are not Java 27 specification claims. */
public final class Java27Example {
    private Java27Example() {}

    public static JavaSourceLanguage language() {
        return Language.INSTANCE;
    }

    public static JavaParser parser() {
        return new JavaParser(syntaxParser());
    }

    public static JavaSyntaxParser syntaxParser() {
        return new JavaSyntaxParser(language(), JavaSyntaxParser.defaultBudget());
    }

    public static JavaSourceProfile profile() {
        JavaSpecificationCatalog baseline = JavaSpecificationCatalog.standard();
        JavaRelease release = new JavaRelease(Java27Names.RELEASE);
        Map<JavaRelease, ClassFileVersion> releases = new HashMap<>();
        for (JavaRelease known : baseline.knownReleases()) {
            releases.put(known, baseline.classFileVersion(known));
        }
        releases.put(release, new ClassFileVersion(Java27Names.CLASSFILE_MAJOR, 0));
        Map<JavaFeatureId, JavaFeatureDefinition> features = new HashMap<>(baseline.features());
        features.put(
                Java27Names.FEATURE,
                new JavaFeatureDefinition(
                        Java27Names.FEATURE, Optional.of(release), Set.of(), Optional.empty()));
        return new JavaSourceProfile(
                release,
                PreviewPolicy.disabled(),
                new JavaSpecificationCatalog(
                        Java27Names.SPECIFICATION,
                        release,
                        releases,
                        features,
                        baseline.classFileRules()));
    }

    private static final class Language {
        private static final JavaSourceLanguage INSTANCE =
                JavaSourceLanguage.builder().install(new Java27SourceExtension()).build();
    }
}
