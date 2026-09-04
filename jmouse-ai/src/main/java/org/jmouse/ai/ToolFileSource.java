package org.jmouse.ai;

/**
 * <strong>Where the bytes of a file reach a tool from.</strong>
 *
 * <h2>⚠️ A capability, because the answer is an installation's and not this library's</h2>
 *
 * <p>A caller holding a photograph sends it encoded. A caller running beside the server may name a
 * file on its disk. A caller whose files already live in an object store has neither — it has a key,
 * and the bytes never pass through the conversation at all. These are not three variations on one
 * mechanism; they are three different places bytes come from, and which of them an installation offers
 * is a deployment decision.
 *
 * <p>This existed as one class with {@code Files.readAllBytes} inside it. That class was a
 * <em>local filesystem</em> wearing the name of the general idea: adding an object-store source meant
 * editing it, and a product whose storage was S3 had no way to say so. The seam is here rather than
 * inside, so a new source is a new class and nothing that reads files is touched.
 *
 * <h2>⚠️ Deliberately not sealed</h2>
 *
 * <p>The whole point is that an application can add one — an S3 source belongs wherever the S3 client
 * already is, not here. A closed hierarchy would put this library in the way of exactly the extension
 * it exists to allow.
 *
 * <h2>Contract</h2>
 *
 * <p>A source owns one argument. It says whether a call is using it, and produces the bytes when it
 * is. It never decides whether the caller <em>may</em> — that is the tool's permission — and it never
 * decides which source wins when two are present, which is {@link ToolFileBytes}'s.
 */
public interface ToolFileSource {

    /**
     * The one argument this source reads, as a caller writes it.
     *
     * <p>⚠️ Two sources sharing an argument name would make {@link #carriedBy} ambiguous, and the
     * ambiguity would read as "both were sent" — a refusal nobody could act on. Names are the
     * library's to keep distinct, and {@link ToolFileBytes} refuses a roster that repeats one.
     *
     * @return the argument name
     */
    String argument();

    /**
     * What a model is told about this argument, in the schema.
     *
     * <p>⚠️ Written by the source rather than by each tool, because these descriptions <em>are</em> the
     * instructions: three hand-copied wordings of "prefer bytes unless you know the path form is on"
     * were three chances for the weakest one to invite the wrong guess.
     *
     * @return the description
     */
    String description();

    /**
     * Whether this call is sending its file through this source.
     *
     * @param invocation the call
     * @return true when the argument is present and not blank
     */
    boolean carriedBy(ToolInvocation invocation);

    /**
     * The bytes.
     *
     * <p>Called only when {@link #carriedBy} answered true.
     *
     * @param invocation the call
     * @return the bytes
     * @throws ToolRefusedException when what was sent cannot be turned into bytes, with a message
     *                              telling the caller what to send instead
     */
    byte[] read(ToolInvocation invocation);
}
