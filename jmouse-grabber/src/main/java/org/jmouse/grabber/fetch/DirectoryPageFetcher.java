package org.jmouse.grabber.fetch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.function.UnaryOperator;

import org.jmouse.core.MediaType;
import org.jmouse.grabber.GrabberException;
import org.jmouse.http.Headers;
import org.jmouse.http.HttpStatus;

/**
 * 📁 Serves pages from a directory rather than from the network.
 *
 * <p>Two things need this. A smoke that has to produce the same answer every time cannot depend on a
 * live site, and a development loop on one handler should not fetch the same page fifty times while
 * the selector is being got right.</p>
 *
 * <p>The address is turned into a file name by a function the caller supplies, so the mapping stays
 * whatever the caller's fixtures are named. An address with no file behind it answers <b>404</b>
 * rather than throwing, because that is what the network would do and the run's own handling of a 404
 * is then exercised too.</p>
 */
final class DirectoryPageFetcher implements PageFetcher {

    private final Path                  directory;
    private final UnaryOperator<String> addressToFile;

    DirectoryPageFetcher(Path directory, UnaryOperator<String> addressToFile) {
        this.directory = directory;
        this.addressToFile = addressToFile;
    }

    @Override
    public PageResponse fetch(PageRequest request) {
        String fileName = addressToFile.apply(request.address().toString());
        Path   file     = directory.resolve(fileName);

        if (!Files.isRegularFile(file)) {
            return new PageResponse(
                    request.address(), request.address(), HttpStatus.NOT_FOUND,
                    new Headers(), new byte[0], Duration.ZERO);
        }

        try {
            byte[]  body    = Files.readAllBytes(file);
            Headers headers = new Headers();

            headers.setContentType(mediaTypeOf(file));
            headers.setStatus(HttpStatus.OK);

            return new PageResponse(
                    request.address(), request.address(), HttpStatus.OK, headers, body, Duration.ZERO);
        } catch (IOException exception) {
            throw new GrabberException("Cannot read the fixture " + file, exception);
        }
    }

    /**
     * 🎯 What a fixture claims to be, from its extension.
     *
     * <p>Deliberately a short list rather than {@code MediaTypeFactory}: that one throws on a name
     * with no extension and answers from an empty list on an unknown one, and a fixture directory is
     * exactly where both of those happen. Anything unrecognised is HTML, which is what a fixture
     * almost always is.</p>
     */
    private MediaType mediaTypeOf(Path file) {
        String name = file.getFileName().toString().toLowerCase(java.util.Locale.ROOT);

        if (name.endsWith(".json")) {
            return MediaType.APPLICATION_JSON;
        }

        if (name.endsWith(".xml")) {
            return MediaType.APPLICATION_XML;
        }

        if (name.endsWith(".txt")) {
            return MediaType.TEXT_PLAIN;
        }

        return MediaType.TEXT_HTML;
    }

}
