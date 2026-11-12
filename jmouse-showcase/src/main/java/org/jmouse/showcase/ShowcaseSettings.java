package org.jmouse.showcase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;

/** Local startup settings and development credentials, kept outside version control. */
final class ShowcaseSettings {

    private final Path dataDirectory;
    private final int port;
    private final Properties credentials;

    private ShowcaseSettings(Path dataDirectory, int port, Properties credentials) {
        this.dataDirectory = dataDirectory;
        this.port = port;
        this.credentials = credentials;
    }

    static ShowcaseSettings load() throws IOException {
        Path dataDirectory = Path.of(environment("JMOUSE_SHOWCASE_DATA", "data")).toAbsolutePath();
        Files.createDirectories(dataDirectory);
        Properties credentials = loadCredentials(dataDirectory.resolve("credentials.properties"));
        int port = Integer.parseInt(environment("JMOUSE_SHOWCASE_PORT", "8092"));
        return new ShowcaseSettings(dataDirectory, port, credentials);
    }

    Path credentialFile() {
        return dataDirectory.resolve("credentials.properties");
    }

    Path databaseFile() {
        return dataDirectory.resolve("showcase.mv.db");
    }

    String jdbcUrl() {
        String databasePath = dataDirectory.resolve("showcase").toString().replace('\\', '/');
        return "jdbc:h2:file:" + databasePath + ";DB_CLOSE_ON_EXIT=FALSE";
    }

    int port() {
        return port;
    }

    String secret(String name) {
        String environmentName = "JMOUSE_SHOWCASE_" + name.toUpperCase(Locale.ROOT).replace('.', '_');
        return environment(environmentName, credentials.getProperty(name));
    }

    private static Properties loadCredentials(Path credentialFile) throws IOException {
        Properties credentials = new Properties();
        if (Files.exists(credentialFile)) {
            try (var input = Files.newInputStream(credentialFile)) {
                credentials.load(input);
            }
        }

        boolean changed = false;
        for (String name : List.of("user.password", "admin.password", "database.password")) {
            if (!credentials.containsKey(name)) {
                credentials.setProperty(name, UUID.randomUUID().toString());
                changed = true;
            }
        }

        if (changed) {
            try (var output = Files.newOutputStream(credentialFile)) {
                credentials.store(
                    output,
                    "Local showcase credentials. Users: demo and admin. Keep this file private."
                );
            }
        }
        return credentials;
    }

    private static String environment(String name, String fallback) {
        return System.getenv().getOrDefault(name, fallback);
    }
}
