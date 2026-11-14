package org.jmouse.cad.document.part;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * 🔎 An {@linkplain InstalledLibrary installed library} as a place parts are found — the same for every format that installs one:
 * read once, in the background, as the engine comes up ({@link #prepare()}) or on the first question, the provider
 * {@link ProviderState.Status#INDEXING} meanwhile; then every part scored against the query's words, the best first.
 *
 * <p>A format brings only what it alone knows — whether its folders are there, what they hold, how one part becomes documents
 * —; a plugin contributes the provider made of it to {@link PartSearchProvider#POINT}.</p>
 */
public final class InstalledLibraryParts implements PartSearchProvider {

    /** Where the reading of the library stands: once read — or failed — it stays so for this provider. */
    private sealed interface Reading {
    }

    private record Unread() implements Reading {
    }

    private record InProgress() implements Reading {
    }

    private record Read(List<IndexedPart> parts) implements Reading {
    }

    private record Failed(String reason) implements Reading {
    }

    private record Scored(IndexedPart part, int score) {
    }

    private final InstalledLibrary library;
    private final Executor reading;
    private final AtomicReference<Reading> state = new AtomicReference<>(new Unread());

    /** @param reading where the library is read: a thread of its own, so nobody waits on it */
    public InstalledLibraryParts(InstalledLibrary library, Executor reading) {
        this.library = library;
        this.reading = reading;
    }

    @Override
    public String id() {
        return library.id();
    }

    @Override
    public String name() {
        return library.name();
    }

    @Override
    public void prepare() {
        if (library.present()) {
            startReading();
        }
    }

    @Override
    public ProviderState state() {
        if (!library.present()) {
            return ProviderState.notConfigured(library.absence());
        }
        return switch (startReading()) {
            case Read read -> ProviderState.ready(summaryOf(read.parts()) + library.license().map(terms -> ". Fetched parts stay under " + terms)
                    .orElse(". Fetched parts stay under the terms of the library each comes from") + ".");
            case Failed failed -> ProviderState.failed(failed.reason());
            case Unread unread -> indexing();
            case InProgress inProgress -> indexing();
        };
    }

    @Override
    public Set<PartCandidate.Kind> offers() {
        return library.offers();
    }

    @Override
    public List<PartCandidate> search(String query, Set<PartCandidate.Kind> kinds, int limit) {
        PartQuery words = PartQuery.of(query);
        if (words.isEmpty() || !(startReading() instanceof Read read)) {
            return List.of();
        }
        return read.parts().stream().filter(part -> kinds.contains(part.candidate().kind()))
                .map(part -> new Scored(part, part.score(words))).filter(scored -> scored.score() > 0)
                .sorted(Comparator.comparingInt(Scored::score).reversed().thenComparing(scored -> scored.part().candidate().identifier()))
                .limit(limit).map(scored -> scored.part().candidate()).toList();
    }

    @Override
    public FetchedPart fetch(String identifier) {
        return library.fetch(identifier);
    }

    private ProviderState indexing() {
        return ProviderState.indexing("Reading the installed " + library.name() + " — the first time takes a little while.");
    }

    /** The reading as it stands, started first when nothing has started it yet. */
    private Reading startReading() {
        Reading now = state.get();
        if (now instanceof Unread unread && state.compareAndSet(unread, new InProgress())) {
            reading.execute(() -> {
                try {
                    state.set(new Read(List.copyOf(library.read())));
                } catch (RuntimeException unreadable) {
                    state.set(new Failed("The installed " + library.name() + " could not be read: " + unreadable.getMessage()));
                }
            });
            return state.get();
        }
        return now;
    }

    /** How many parts of each kind it holds: {@code 22905 components, 15464 footprints}. */
    private static String summaryOf(List<IndexedPart> parts) {
        Map<PartCandidate.Kind, Long> counted = parts.stream().collect(Collectors.groupingBy(part -> part.candidate().kind(),
                () -> new EnumMap<>(PartCandidate.Kind.class), Collectors.counting()));
        if (counted.isEmpty()) {
            return "No parts";
        }
        return counted.entrySet().stream().map(entry -> {
            String kind = entry.getKey().name().toLowerCase(Locale.ROOT);
            return entry.getValue() + " " + (entry.getValue() == 1 ? kind : kind + "s");
        }).collect(Collectors.joining(", "));
    }
}
