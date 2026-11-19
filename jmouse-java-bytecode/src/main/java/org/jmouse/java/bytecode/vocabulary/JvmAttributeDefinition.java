package org.jmouse.java.bytecode.vocabulary;

import org.jmouse.java.model.version.ClassFileVersion;
import org.jmouse.java.model.version.JavaFeatureDefinition;
import org.jmouse.java.model.version.JavaFeatureId;
import org.jmouse.java.model.version.JavaSpecificationCatalog;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record JvmAttributeDefinition(
        String name,
        int major,
        int minor,
        String section,
        String node,
        Set<JvmAttributeContext> contexts,
        JvmAttributeMultiplicity multiplicity,
        Optional<JavaFeatureId> feature) {
    public JvmAttributeDefinition {
        contexts = Set.copyOf(contexts);
        Objects.requireNonNull(feature, "feature");
    }

    public JvmAttributeDefinition(
            String name,
            int major,
            int minor,
            String section,
            String node,
            Set<JvmAttributeContext> contexts,
            JvmAttributeMultiplicity multiplicity) {
        this(name, major, minor, section, node, contexts, multiplicity, Optional.empty());
    }

    public boolean availableIn(ClassFileVersion version, JavaSpecificationCatalog specification) {
        if (feature.isEmpty()) {
            return version.major() >= major;
        }
        JavaFeatureDefinition definition = specification.feature(feature.orElseThrow());
        if (definition.retiredSince().isPresent()
                && version.major()
                        >= specification
                                .classFileVersion(definition.retiredSince().orElseThrow())
                                .major()) {
            return false;
        }
        if (definition.standardSince().isPresent()
                && version.major()
                        >= specification
                                .classFileVersion(definition.standardSince().orElseThrow())
                                .major()) {
            return true;
        }
        return specification.classFileRules().isPreview(version)
                && definition.previewReleases().stream()
                        .anyMatch(
                                release ->
                                        specification.classFileVersion(release).major()
                                                == version.major());
    }
}
