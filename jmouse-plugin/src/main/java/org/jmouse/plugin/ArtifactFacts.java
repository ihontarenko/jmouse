package org.jmouse.plugin;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

/**
 * What a library says of itself in its jar's manifest — its title, version, maker, licence and description —, read for
 * a class it holds. A class run from a folder of classes, not a jar, says nothing: every fact is then null.
 *
 * @param title       {@code Implementation-Title}
 * @param version     {@code Implementation-Version}
 * @param author      {@code Implementation-Vendor}
 * @param license     {@code Bundle-License}
 * @param description {@code Bundle-Description}
 */
public record ArtifactFacts(String title, String version, String author, String license, String description) {

    private static final String MANIFEST = "META-INF/MANIFEST.MF";

    public static final ArtifactFacts NONE = new ArtifactFacts(null, null, null, null, null);

    /** The facts of the jar that holds this class. */
    public static ArtifactFacts of(Class<?> holder) {
        URL classFile = holder.getResource(holder.getSimpleName() + ".class");
        if (classFile == null || !"jar".equals(classFile.getProtocol())) {
            return NONE;
        }
        String jar = classFile.toExternalForm();
        String manifestAddress = jar.substring(0, jar.indexOf("!/") + 2) + MANIFEST;
        try (InputStream input = URI.create(manifestAddress).toURL().openStream()) {
            Attributes main = new Manifest(input).getMainAttributes();
            return new ArtifactFacts(main.getValue("Implementation-Title"), main.getValue("Implementation-Version"),
                                     main.getValue("Implementation-Vendor"), main.getValue("Bundle-License"),
                                     main.getValue("Bundle-Description"));
        } catch (IOException | IllegalArgumentException unreadable) {
            return NONE;
        }
    }
}
