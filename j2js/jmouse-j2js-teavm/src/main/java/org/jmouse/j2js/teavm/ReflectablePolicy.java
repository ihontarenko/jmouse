package org.jmouse.j2js.teavm;

import org.jmouse.j2js.Reflectable;
import org.teavm.extension.spi.reflection.SimpleReflectionPolicy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 🌐 The TeaVM binding of {@link Reflectable}: every member of a class so marked — fields, methods, constructors, private ones
 * too, as a record's components are — is kept for reflection, and of every class in a package a module names in
 * {@value Reflectable#PACKAGES_RESOURCE}; no member of any other class is. Found by TeaVM on the class
 * path it compiles ({@code META-INF/services/org.teavm.extension.spi.reflection.ReflectionPolicy}).
 *
 * @author Ivan Hontarenko (Mr. Jerry Mouse)
 * @author ihontarenko@gmail.com
 */
public final class ReflectablePolicy extends SimpleReflectionPolicy {

    @Override
    protected void setup() {
        selectClasses(withAnnotation(Reflectable.class)).reflectableMembers(member -> true);

        for (String reflectable : reflectablePackages()) {
            selectPackage(reflectable, true).reflectableMembers(member -> true);
        }
    }

    /** Every package named on the class path TeaVM compiles, each once. */
    private Set<String> reflectablePackages() {
        Set<String> packages = new LinkedHashSet<>();
        try {
            for (URL resource : Collections.list(ReflectablePolicy.class.getClassLoader().getResources(Reflectable.PACKAGES_RESOURCE))) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.openStream(), StandardCharsets.UTF_8))) {
                    reader.lines().map(line -> line.replaceFirst("#.*", "").strip()).filter(line -> !line.isEmpty()).forEach(packages::add);
                }
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("The packages named in " + Reflectable.PACKAGES_RESOURCE + " could not be read", exception);
        }
        return packages;
    }
}
