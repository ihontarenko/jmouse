package svit.io;

import svit.matcher.Matcher;
import svit.util.Files;
import svit.util.Jars;

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
        return new JarURLResource(createJarURL(location));
    }

    @Override
    public Collection<Resource> loadResources(String location, Matcher<Resource> matcher) {
        Collection<Resource> resources = new ArrayList<>();

        location = Files.removeProtocol(location);

        try {
            Enumeration<URL> enumeration = getClassLoader().getResources(location);

            while (enumeration.hasMoreElements()) {
                URL url = enumeration.nextElement();
                if (supports(url.getProtocol())) {
                    resources.addAll(loadJarResources(location, url, matcher));
                } else {
                    resources.add(new URLResource(url));
                }
            }

        } catch (IOException e) {
            throw new ResourceException("Failed to read resources from '%s' files".formatted(location), e);
        }

        return resources;
    }

    private Collection<Resource> loadJarResources(String location, URL jar, Matcher<Resource> matcher) {
        Collection<Resource> resources = new ArrayList<>();

        try {
            JarURLConnection connection = (JarURLConnection) jar.openConnection();

            try (JarFile file = connection.getJarFile()) {

                Enumeration<JarEntry> entries = file.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();

                    if (!entry.isDirectory() && entry.getName().startsWith(location)) {
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

    public URL createJarURL(String location) {
        try {
            return Jars.isJarURL(location) ? new URI(location).toURL()
                    : getClassLoader().getResource(Files.removeProtocol(location));
        } catch (MalformedURLException | URISyntaxException e) {
            throw new JarResourceException("Failed to create URL from '%s' location".formatted(location), e);
        }
    }

    @Override
    public List<String> supportedProtocols() {
        return List.of(Resource.JAR_FILE);
    }

}
