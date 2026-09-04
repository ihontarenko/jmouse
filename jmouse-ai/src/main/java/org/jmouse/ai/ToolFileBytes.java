package org.jmouse.ai;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The sources a file may reach a tool through, and the one rule that spans them.
 *
 * <h2>⚠️ Here because three products had written it separately</h2>
 *
 * <p>Tessera carried it as a class, Kiwi inline, Innoventa was about to be the third. Every one of them
 * answered <em>may this server read that path</em> for itself, and the answer that matters once they
 * drift is the loosest of the three — which is precisely the shape a check on a filesystem must not
 * have. One implementation cannot drift from itself.
 *
 * <h2>What is here, and what is not</h2>
 *
 * <p>This holds the <strong>invariant</strong>: exactly one source, never none and never two. Where the
 * bytes actually come from is a {@link ToolFileSource}, and this knows nothing about any of them —
 * which is what lets an installation add an object-store source without this class hearing about it.
 *
 * <p>⚠️ <strong>Storing the file is somewhere else entirely.</strong> These sources are the
 * <em>ingress</em>: how bytes arrive at a tool. Where they are then kept is the application's storage
 * layer, which has its own abstraction and its own S3 implementation. Confusing the two is easy and
 * costly — a product whose storage is S3 still needs an answer to "the person just pasted a
 * photograph", and that answer is {@link EncodedFileSource}.
 *
 * <h2>⚠️ Not a component, and the application composes it</h2>
 *
 * <p>This library depends on no framework, so there is nothing here to read configuration with. An
 * application declares one of these as a bean with whichever sources it offers — which also means each
 * product keeps its existing property names rather than migrating to a shared one.
 *
 * <pre>{@code
 * @Bean
 * ToolFileBytes toolFileBytes(@Value("${innoventa.mcp.upload-root:}") String uploadRoot) {
 *     return uploadRoot.isBlank()
 *             ? ToolFileBytes.encodedOnly()
 *             : ToolFileBytes.of(new EncodedFileSource(), new LocalDirectoryFileSource(uploadRoot));
 * }
 * }</pre>
 */
public final class ToolFileBytes {

    private final List<ToolFileSource> sources;

    private ToolFileBytes(List<ToolFileSource> sources) {
        this.sources = List.copyOf(sources);
    }

    /**
     * The sources this installation offers, in the order a schema should list them.
     *
     * @param sources at least one
     * @return the roster
     * @throws IllegalArgumentException when none are given, or two claim the same argument
     */
    public static ToolFileBytes of(ToolFileSource... sources) {
        List<ToolFileSource> offered = List.of(sources);

        if (offered.isEmpty()) {
            throw new IllegalArgumentException(
                    "A file needs at least one source to arrive through. EncodedFileSource works "
                    + "everywhere and needs no configuration.");
        }

        // ⚠️ Refused at construction rather than discovered at a call. Two sources on one argument
        // makes "exactly one was sent" unanswerable, and the failure would land on a caller who did
        // nothing wrong — at a moment when nobody is looking at the configuration.
        Set<String> arguments = new LinkedHashSet<>();

        offered.forEach(source -> {
            if (!arguments.add(source.argument())) {
                throw new IllegalArgumentException(
                        "Two file sources both read '" + source.argument() + "'. Each source owns one "
                        + "argument.");
            }
        });

        return new ToolFileBytes(offered);
    }

    /** Bytes in the call and nothing else — the roster for an installation that reads no files. */
    public static ToolFileBytes encodedOnly() {
        return of(new EncodedFileSource());
    }

    /**
     * The bytes this call is carrying, from whichever source it used.
     *
     * @param invocation the call
     * @return the bytes
     * @throws ToolRefusedException when no source was used, or more than one was
     */
    public byte[] of(ToolInvocation invocation) {
        List<ToolFileSource> used = sources.stream()
                .filter(source -> source.carriedBy(invocation))
                .toList();

        if (used.size() == 1) {
            return used.getFirst().read(invocation);
        }

        /*
         * ⚠️ Both is refused as firmly as neither, and it is the more dangerous of the two. Two
         * sources for one file means one of them is silently ignored — and a caller who sent a path
         * and received the bytes of something else has no way to find out.
         */
        throw new ToolRefusedException(RefusalReason.INVALID_ARGUMENT,
                used.isEmpty()
                        ? "No file was sent. Use exactly one of: " + argumentList() + "."
                        : "A file was sent more than one way (" + names(used) + "). Use exactly one.");
    }

    /**
     * Each source's argument, in order, for a schema to declare.
     *
     * <p>⚠️ Every one is optional even though a file is required, because <em>which</em> is required is
     * the invariant above rather than anything a JSON schema can say. {@link #of} is what enforces it,
     * and it is the only place that can.
     *
     * @return argument name to the description a model reads
     */
    public Map<String, String> arguments() {
        Map<String, String> described = new LinkedHashMap<>();

        sources.forEach(source -> described.put(source.argument(), describe(source)));

        return described;
    }

    /**
     * The description of one source, told what else is on offer.
     *
     * <p>A source cannot write "or send a path instead" for itself — it does not know whether a path
     * source exists in this installation. The roster does, so it is the roster that says so.
     */
    private String describe(ToolFileSource source) {
        List<String> others = sources.stream()
                .map(ToolFileSource::argument)
                .filter(argument -> !argument.equals(source.argument()))
                .toList();

        if (others.isEmpty()) {
            return source.description();
        }

        return source.description() + " Send this or " + quoted(others) + ", never both.";
    }

    private String argumentList() {
        return quoted(sources.stream().map(ToolFileSource::argument).toList());
    }

    private String names(List<ToolFileSource> used) {
        return quoted(used.stream().map(ToolFileSource::argument).toList());
    }

    private static String quoted(List<String> arguments) {
        return String.join(", ", arguments.stream().map(argument -> "'" + argument + "'").toList());
    }
}
