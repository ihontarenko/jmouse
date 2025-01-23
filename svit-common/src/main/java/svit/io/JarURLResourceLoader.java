package svit.io;

import svit.matcher.Matcher;
import svit.util.Files;
import svit.util.Jars;
import svit.util.JavaIO;

import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class JarURLResourceLoader extends AbstractResourceLoader {

    @Override
    public Resource getResource(String location) {
        return new JarURLResource(JavaIO.toURL(location, getClassLoader()));
    }

    @Override
    public Collection<Resource> loadResources(String location, Matcher<String> matcher) {
        Collection<Resource> resources = new ArrayList<>();

        location = Files.removeProtocol(location);

        try {
            Enumeration<URL> enumeration = getClassLoader().getResources(location);

            while (enumeration.hasMoreElements()) {
                URL url = enumeration.nextElement();
                if (supports(url.getProtocol())) {
                    resources.addAll(loadResources(location, url, matcher));
                }
            }

        } catch (IOException e) {
            throw new ResourceException("Failed to read resources from '%s' files".formatted(location), e);
        }

        return resources;
    }

    public Collection<Resource> loadResources(String location, URL jar, Matcher<String> matcher) {
        Collection<Resource> resources = new ArrayList<>();

        try {
            JarURLConnection connection = (JarURLConnection) jar.openConnection();

            try (JarFile file = connection.getJarFile()) {

                Enumeration<JarEntry> entries = file.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();

                    if (!entry.isDirectory() && matcher.matches(entry.getName())) {
                        String entryLocation = "%s%s%s".formatted(Jars.getBasePath(jar), Jars.JAR_TOKEN, entry.getName());
                        resources.add(getResource(entryLocation));
                    }
                }
            }
        } catch (Exception exception) {
            throw new JarResourceException("Failed to read jar resources from '%s' files".formatted(jar), exception);
        }

        return resources;
    }

    @Override
    public List<String> supportedProtocols() {
        return List.of(Resource.JAR_PROTOCOL);
    }

}
