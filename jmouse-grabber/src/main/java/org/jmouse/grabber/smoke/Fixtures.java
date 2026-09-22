package org.jmouse.grabber.smoke;

import java.net.URI;
import java.nio.file.Path;
import java.util.function.UnaryOperator;

import org.jmouse.grabber.fetch.PageFetcher;

/**
 * 🧪 The fixture site every smoke here runs against.
 *
 * <p>Five pages on disk, served through {@link PageFetcher#fromDirectory}. No network, so the smokes
 * produce the same answer on a train as on a desk, and a page that does not exist answers 404 exactly
 * as the network would.</p>
 *
 * <pre>
 * /catalog          two product cards, a next-page link carrying a tracking parameter
 * /catalog?page=2   one more card, no next link
 * /product/1..3     a title, a price, a stock keeping unit, two images
 * </pre>
 */
final class Fixtures {

    /**
     * 🌐 The invented host. Nothing resolves it; the fetcher never leaves the disk.
     */
    static final String SITE = "https://parts.example.com";

    static final URI CATALOG = URI.create(SITE + "/catalog");

    private static final Path DIRECTORY = Path.of("jmouse-grabber", "src", "main", "resources", "grabber-fixtures");

    private Fixtures() {
    }

    /**
     * 📁 A fetcher serving the fixture directory.
     *
     * <p>⚠️ The path is relative to the reactor root, because that is where a {@code main} in this
     * repository is run from. A smoke started from the module directory finds nothing and every page
     * answers 404 — which is the first thing to check if one of them reports zero pages.</p>
     */
    static PageFetcher fetcher() {
        return PageFetcher.fromDirectory(directory(), addressToFile());
    }

    static Path directory() {
        Path fromReactorRoot = DIRECTORY;

        if (fromReactorRoot.toFile().isDirectory()) {
            return fromReactorRoot;
        }

        return Path.of("src", "main", "resources", "grabber-fixtures");
    }

    /**
     * 🗺️ Which file answers which address.
     */
    static UnaryOperator<String> addressToFile() {
        return address -> {
            String path = address.substring(SITE.length());

            if (path.startsWith("/product/")) {
                return "product-" + path.substring("/product/".length()) + ".html";
            }

            if (path.startsWith("/catalog?page=2")) {
                return "catalog-2.html";
            }

            if (path.startsWith("/catalog")) {
                return "catalog.html";
            }

            return "nothing-here.html";
        };
    }

}
