package org.jmouse.pipeline.definition.loading;

import org.jmouse.core.Verify;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FileSource implements DefinitionSource {

    private final Path path;

    public FileSource(Path path) {
        this.path = Verify.nonNull(path, "path");
    }

    @Override
    public String location() {
        return "file:" + path.toAbsolutePath();
    }

    @Override
    public InputStream openStream() {
        try {
            return Files.newInputStream(path);
        } catch (IOException e) {
            throw new IllegalArgumentException("Cannot open file: " + path, e);
        }
    }
}
