package org.jmouse.java.model.version;

import java.io.InputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.TreeMap;

/** Immutable specification metadata shared by source parsing and binary admission. */
public final class JavaSpecificationCatalog {

    private final String identity;
    private final JavaRelease defaultRelease;
    private final Map<JavaRelease, ClassFileVersion> releases;
    private final Map<JavaFeatureId, JavaFeatureDefinition> features;
    private final ClassFileVersionRules classFileRules;
    private final String fingerprint;

    public JavaSpecificationCatalog(String identity, JavaRelease defaultRelease,
            Map<JavaRelease, ClassFileVersion> releases, Map<JavaFeatureId, JavaFeatureDefinition> features,
            ClassFileVersionRules classFileRules) {
        this.identity = Objects.requireNonNull(identity, "identity");
        this.defaultRelease = Objects.requireNonNull(defaultRelease, "defaultRelease");
        this.releases = Map.copyOf(releases);
        this.features = Map.copyOf(features);
        this.classFileRules = Objects.requireNonNull(classFileRules, "classFileRules");
        if (identity.isBlank() || !this.releases.containsKey(defaultRelease)) {
            throw new IllegalArgumentException("A specification needs an identity and a known default release");
        }
        for (var entry : this.features.entrySet()) {
            if (!entry.getKey().equals(entry.getValue().identity())) {
                throw new IllegalArgumentException("Feature key differs from feature identity");
            }
        }
        fingerprint = fingerprint();
    }

    public static JavaSpecificationCatalog standard() {
        return Standard.INSTANCE;
    }

    public String identity() {
        return identity;
    }

    public JavaRelease defaultRelease() {
        return defaultRelease;
    }

    public String specificationFingerprint() {
        return fingerprint;
    }

    public ClassFileVersionRules classFileRules() {
        return classFileRules;
    }

    public ClassFileVersion classFileVersion(JavaRelease release) {
        ClassFileVersion version = releases.get(release);
        if (version == null) {
            throw new IllegalArgumentException("Unknown Java specification release " + release.number());
        }
        return version;
    }

    public Set<JavaRelease> knownReleases() {
        return releases.keySet();
    }

    public Map<JavaFeatureId, JavaFeatureDefinition> features() {
        return features;
    }

    public JavaFeatureDefinition feature(JavaFeatureId identity) {
        JavaFeatureDefinition feature = features.get(identity);
        if (feature == null) {
            throw new IllegalArgumentException("Unknown Java feature " + identity.name());
        }
        return feature;
    }

    private String fingerprint() {
        StringBuilder canonical = new StringBuilder(identity).append('\n').append(defaultRelease.number())
                .append('\n').append(classFileRules).append('\n');
        new TreeMap<>(releases).forEach((release, version) -> canonical.append(release.number())
                .append('=').append(version.major()).append('.').append(version.minor()).append('\n'));
        features.values().stream().sorted(Comparator.comparing(feature -> feature.identity().name()))
                .forEach(feature -> canonical.append(feature.identity().name()).append('=')
                        .append(feature.standardSince().map(JavaRelease::number).orElse(null)).append(':')
                        .append(feature.previewReleases().stream().sorted().map(JavaRelease::number).toList())
                        .append(':').append(feature.retiredSince().map(JavaRelease::number).orElse(null)).append('\n'));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("The Java platform must provide SHA-256", exception);
        }
    }

    private static JavaSpecificationCatalog readStandard() {
        Properties properties = new Properties();
        try (InputStream input = JavaSpecificationCatalog.class.getResourceAsStream("/org/jmouse/java/model/specification.properties")) {
            if (input == null) {
                throw new IllegalStateException("Java specification metadata is missing");
            }
            properties.load(input);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read Java specification metadata", exception);
        }
        Map<JavaRelease, ClassFileVersion> releases = new TreeMap<>();
        Map<JavaFeatureId, JavaFeatureDefinition> features = new LinkedHashMap<>();
        for (String key : properties.stringPropertyNames().stream().sorted().toList()) {
            if (key.startsWith("release.")) {
                JavaRelease release = new JavaRelease(Integer.parseInt(key.substring("release.".length())));
                String[] components = properties.getProperty(key).split("\\.", -1);
                if (components.length != 2) {
                    throw new IllegalStateException("Invalid classfile version for " + key);
                }
                releases.put(release, new ClassFileVersion(Integer.parseInt(components[0]), Integer.parseInt(components[1])));
            } else if (key.startsWith("feature.") && key.endsWith(".standard")) {
                String name = key.substring("feature.".length(), key.length() - ".standard".length());
                JavaFeatureId identity = new JavaFeatureId(name);
                String prefix = "feature." + name;
                Set<JavaRelease> preview = Arrays.stream(properties.getProperty(prefix + ".preview", "").split(","))
                        .map(String::strip).filter(value -> !value.isEmpty())
                        .map(value -> new JavaRelease(Integer.parseInt(value))).collect(Collectors.toUnmodifiableSet());
                features.put(identity, new JavaFeatureDefinition(identity, optionalRelease(properties.getProperty(key)),
                        preview, optionalRelease(properties.getProperty(prefix + ".retired"))));
            }
        }
        return new JavaSpecificationCatalog(properties.getProperty("identity"),
                new JavaRelease(Integer.parseInt(properties.getProperty("default.release"))), releases, features,
                new ClassFileVersionRules(Integer.parseInt(properties.getProperty("format.minimum-major")),
                        Integer.parseInt(properties.getProperty("format.preview-minimum-major")),
                        Integer.parseInt(properties.getProperty("format.preview-minor"))));
    }

    private static Optional<JavaRelease> optionalRelease(String value) {
        return value == null || value.isBlank() ? Optional.empty()
                : Optional.of(new JavaRelease(Integer.parseInt(value.strip())));
    }

    private static final class Standard {
        private static final JavaSpecificationCatalog INSTANCE = readStandard();
    }
}
