package org.jmouse.grabber.journal;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jmouse.grabber.Attributes;
import org.jmouse.grabber.GrabberException;
import org.jmouse.grabber.Visit;
import org.jmouse.grabber.VisitKey;
import org.jmouse.grabber.VisitOrigin;

/**
 * 💾 A journal on disk, so a run that dies resumes rather than starts over.
 *
 * <p>Three append-only files in one directory. Appending rather than rewriting is the point: a
 * process killed mid-write loses at most its last line, and a journal that rewrote itself could lose
 * everything at exactly the moment it is needed.</p>
 *
 * <pre>
 * &lt;directory&gt;/seen.log      one visit key per line
 * &lt;directory&gt;/items.log     one item key per line
 * &lt;directory&gt;/pending.log   "+" and a serialised visit when queued, "-" and its key when finished
 * </pre>
 *
 * <p>The whole of it is read into memory on construction and appended to from then on, which is what
 * keeps {@link #seen(VisitKey)} cheap enough to ask on every discovered link.</p>
 *
 * <p>⚠️ <b>An attribute whose value is not text does not survive a restart.</b> The baton is
 * serialised as strings, because a journal that serialised arbitrary objects would need a
 * serialisation library and would still fail on the one object somebody put there. A resumed visit
 * carries the string form; anything richer has to be re-derived. This is worth knowing before
 * putting a parsed object on a baton and expecting it back.</p>
 */
final class FileVisitJournal implements VisitJournal {

    private static final String SEEN_FILE    = "seen.log";
    private static final String ITEMS_FILE   = "items.log";
    private static final String PENDING_FILE = "pending.log";

    private static final char FIELD    = '\t';
    private static final char ADDED    = '+';
    private static final char FINISHED = '-';

    private final Path directory;

    private final Set<String>        seen        = ConcurrentHashMap.newKeySet();
    private final Set<String>        processed   = ConcurrentHashMap.newKeySet();
    private final Map<String, Visit> outstanding = new ConcurrentHashMap<>();

    private final Object writeLock = new Object();

    FileVisitJournal(Path directory) {
        this.directory = directory;

        try {
            Files.createDirectories(directory);
        } catch (IOException exception) {
            throw new GrabberException("Cannot create the journal directory " + directory, exception);
        }

        readSeen();
        readProcessed();
        readPending();
    }

    @Override
    public boolean seen(VisitKey key) {
        return seen.contains(key.value());
    }

    @Override
    public boolean markSeen(VisitKey key) {
        boolean firstSighting = seen.add(key.value());

        if (firstSighting) {
            append(SEEN_FILE, encode(key.value()));
        }

        return firstSighting;
    }

    @Override
    public boolean processed(String itemKey) {
        return processed.contains(itemKey);
    }

    @Override
    public void markProcessed(String itemKey) {
        if (processed.add(itemKey)) {
            append(ITEMS_FILE, encode(itemKey));
        }
    }

    @Override
    public void remember(VisitKey key, Visit visit) {
        outstanding.put(key.value(), visit);
        append(PENDING_FILE, ADDED + String.valueOf(FIELD) + encode(key.value()) + FIELD + serialize(visit));
    }

    @Override
    public void forget(VisitKey key) {
        if (outstanding.remove(key.value()) != null) {
            append(PENDING_FILE, FINISHED + String.valueOf(FIELD) + encode(key.value()));
        }
    }

    @Override
    public List<Visit> pending() {
        return new ArrayList<>(outstanding.values());
    }

    @Override
    public void clear() {
        synchronized (writeLock) {
            seen.clear();
            processed.clear();
            outstanding.clear();

            delete(SEEN_FILE);
            delete(ITEMS_FILE);
            delete(PENDING_FILE);
        }
    }

    @Override
    public long seenCount() {
        return seen.size();
    }

    private void readSeen() {
        eachLine(SEEN_FILE, line -> seen.add(decode(line)));
    }

    private void readProcessed() {
        eachLine(ITEMS_FILE, line -> processed.add(decode(line)));
    }

    private void readPending() {
        eachLine(PENDING_FILE, line -> {
            String[] fields = line.split(String.valueOf(FIELD), -1);

            if (fields.length < 2) {
                return;
            }

            String key = decode(fields[1]);

            if (ADDED == fields[0].charAt(0) && fields.length >= 3) {
                outstanding.put(key, deserialize(fields, 2));
            } else {
                outstanding.remove(key);
            }
        });
    }

    private void eachLine(String fileName, java.util.function.Consumer<String> reader) {
        Path file = directory.resolve(fileName);

        if (!Files.exists(file)) {
            return;
        }

        try (var lines = Files.lines(file, StandardCharsets.UTF_8)) {
            lines.filter(line -> !line.isBlank()).forEach(reader);
        } catch (IOException exception) {
            throw new GrabberException("Cannot read the journal file " + file, exception);
        }
    }

    private void append(String fileName, String line) {
        Path file = directory.resolve(fileName);

        synchronized (writeLock) {
            try (BufferedWriter writer = Files.newBufferedWriter(
                    file, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
                writer.write(line);
                writer.newLine();
            } catch (IOException exception) {
                throw new UncheckedIOException("Cannot append to the journal file " + file, exception);
            }
        }
    }

    private void delete(String fileName) {
        try {
            Files.deleteIfExists(directory.resolve(fileName));
        } catch (IOException exception) {
            throw new GrabberException("Cannot clear the journal file " + fileName, exception);
        }
    }

    /**
     * 📝 address · depth · route · attempt · origin address · origin route · attribute pairs.
     */
    private String serialize(Visit visit) {
        StringBuilder line = new StringBuilder();

        line.append(encode(visit.address().toString())).append(FIELD);
        line.append(visit.depth()).append(FIELD);
        line.append(encode(visit.route())).append(FIELD);
        line.append(visit.attempt()).append(FIELD);
        line.append(encode(originAddress(visit.origin()))).append(FIELD);
        line.append(encode(visit.origin().route()));

        visit.attributes().asMap().forEach((name, value) -> {
            line.append(FIELD).append(encode(name)).append(FIELD).append(encode(String.valueOf(value)));
        });

        return line.toString();
    }

    private Visit deserialize(String[] fields, int offset) {
        URI    address       = URI.create(decode(fields[offset]));
        int    depth         = Integer.parseInt(fields[offset + 1]);
        String route         = decode(fields[offset + 2]);
        int    attempt       = Integer.parseInt(fields[offset + 3]);
        String originAddress = decode(fields[offset + 4]);
        String originRoute   = decode(fields[offset + 5]);

        Map<String, Object> carried = new LinkedHashMap<>();

        for (int index = offset + 6; index + 1 < fields.length; index += 2) {
            carried.put(decode(fields[index]), decode(fields[index + 1]));
        }

        VisitOrigin origin = originAddress == null
                ? VisitOrigin.seed()
                : VisitOrigin.discoveredOn(URI.create(originAddress), originRoute);

        return new Visit(address, depth, route, Attributes.of(carried), origin, attempt);
    }

    private String originAddress(VisitOrigin origin) {
        return origin.address() == null ? null : origin.address().toString();
    }

    private String encode(String value) {
        if (value == null) {
            return "";
        }

        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String decode(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }

        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

}
