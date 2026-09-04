package org.jmouse.storage.policy;

import org.jmouse.storage.Content;
import org.jmouse.storage.ContentTypes;
import org.jmouse.storage.configuration.StorageSettings;
import org.jmouse.storage.configuration.UploadSettings;
import org.jmouse.storage.exception.UploadRejectedException;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 🛃 What may enter storage, and how large it may be.
 *
 * <p>One component in two modes rather than two components: a product that blocks executables and
 * markup, and a product that admits only images, PDF and Office formats, are configuring the same
 * rule in opposite directions.</p>
 *
 * <p>Declared content type and extension are checked <strong>independently</strong>, because a
 * client controls both and may lie about either — an executable renamed {@code invoice.pdf} and an
 * executable declared as {@code application/pdf} are two different lies and each needs its own
 * check.</p>
 *
 * <p>A policy decides; it never writes. Rejected content therefore leaves nothing behind, because
 * nothing was ever opened.</p>
 *
 * <h3>⚠️ A refusal says three things, and the second is the only one a policy cannot work out</h3>
 *
 * <p><em>What is wrong</em> — the type or the extension or the size. <em>Why this destination is
 * narrow</em> — the {@link #reason}, prose written by whoever configured it, because "CAD keeps
 * drawings, a photograph belongs on the part" is not derivable from a list of extensions. <em>What
 * would have worked</em> — the accepted set, summarised.</p>
 *
 * <p>Without the middle one a product has to keep the sentence somewhere of its own and stitch it back
 * on in an interface, which makes two authorities over one question: edit the rule and the sentence
 * still describes the old one. So it travels with the rule, and every path that judges an upload gets
 * it rather than only the screens somebody remembered to decorate.</p>
 */
public final class UploadPolicy {

    private static final long BYTES_PER_MEGABYTE = 1024L * 1024L;

    /**
     * How many accepted values a refusal names before it starts counting instead.
     *
     * <p>⚠️ A folder taking forty CAD extensions is ordinary, and forty of them in a sentence is not a
     * sentence — it is a wall a person stops reading before reaching the part that concerns them. The
     * few that are printed are sorted, so the same rule always names the same ones.</p>
     */
    private static final int NAMED_IN_A_REFUSAL = 8;

    private final AcceptanceMode mode;
    private final Set<String>    contentTypes;
    private final Set<String>    extensions;
    private final long           maxSizeBytes;
    private final String         reason;

    /**
     * 🏗️ Build a policy from its parts, with nothing to say about why it is shaped that way.
     *
     * <p>What an installation-wide policy is: there is no destination to explain, so a refusal is the
     * bare fact and that is the whole truth available.</p>
     *
     * @param mode         how the lists are read
     * @param contentTypes bare {@code type/subtype} values
     * @param extensions   extensions without their dot
     * @param maxSizeBytes largest content accepted
     */
    public UploadPolicy(AcceptanceMode mode, Set<String> contentTypes, Set<String> extensions,
                        long maxSizeBytes) {
        this(mode, contentTypes, extensions, maxSizeBytes, null);
    }

    /**
     * 🏗️ Build a policy that can explain itself.
     *
     * @param mode         how the lists are read
     * @param contentTypes bare {@code type/subtype} values
     * @param extensions   extensions without their dot
     * @param maxSizeBytes largest content accepted
     * @param reason       one sentence saying what this destination is for, addressed to whoever was
     *                     just refused, or {@code null} where nobody wrote one
     */
    public UploadPolicy(AcceptanceMode mode, Set<String> contentTypes, Set<String> extensions,
                        long maxSizeBytes, String reason) {
        this.mode         = mode;
        this.contentTypes = lowerCased(contentTypes);
        this.extensions   = lowerCased(extensions);
        this.maxSizeBytes = maxSizeBytes;
        this.reason       = reason == null || reason.isBlank() ? null : reason.trim();
    }

    /**
     * 🏗️ Build a policy entirely out of configuration — no subclass, no code change, no rebuild.
     *
     * @param settings the active storage settings
     * @return the configured policy
     */
    public static UploadPolicy of(StorageSettings settings) {
        UploadSettings upload = settings.upload().resolve();
        return new UploadPolicy(upload.mode(), upload.contentTypes(), upload.extensions(),
                                settings.maxSizeBytes());
    }

    /**
     * ✅ Decide whether content may be stored.
     *
     * <h3>⚠️ The extension is judged before the content type, and the order is about the message</h3>
     *
     * <p>Both are checked and both refuse, so nothing gets in either way — what the order decides is
     * <strong>which refusal a person reads</strong>. An extension is in the filename they chose; a
     * content type is what their browser guessed and is not visible anywhere. Answering an {@code .mp3}
     * dropped on a CAD shelf with <em>«type 'audio/mpeg' is not accepted; accepted types here:
     * application/vnd.openxmlformats-officedocument.spreadsheetml.sheet…»</em> is true and unusable;
     * naming the extension and the extensions that would work is the same refusal, addressed to
     * somebody who can act on it.</p>
     *
     * @param content the content offered
     * @throws UploadRejectedException describing the first rule the content breaks
     */
    public void accept(Content content) {
        if (content.hasDeclaredSize()) {
            ensureNotEmpty(content.declaredSize());
            ensureWithinSizeLimit(content.declaredSize());
        }

        ensureExtensionAccepted(content.extension());
        ensureContentTypeAccepted(ContentTypes.baseType(content.declaredContentType()));
    }

    /**
     * 📏 Reject content larger than the configured maximum.
     *
     * <p>A negative size means "unknown" — a remote server that omits {@code Content-Length}, a
     * generated stream — and passes. The size a client claims is not evidence of anything, so this
     * is also the method a caller re-runs against
     * {@link org.jmouse.storage.StoredObject#sizeBytes()} once the bytes have actually arrived,
     * deleting the object if the real size breaks the limit a lying declaration slipped past.</p>
     *
     * @param sizeBytes size to check
     * @throws UploadRejectedException when the size exceeds the maximum
     */
    public void ensureWithinSizeLimit(long sizeBytes) {
        if (sizeBytes > maxSizeBytes) {
            // ⚠️ The accepted set is deliberately NOT appended here: a file refused for its size was
            // the right kind, and listing the kinds would send whoever reads it to change the wrong
            // thing about the file.
            throw new UploadRejectedException(withReason(
                    "File size exceeds the maximum of %d MB.".formatted(maxSizeBytes / BYTES_PER_MEGABYTE)));
        }
    }

    /**
     * 🕳️ Reject content with no bytes in it.
     *
     * <p>An empty upload is always a mistake — a failed browser selection, a truncated import — and
     * storing it costs an object that nothing will ever want to read.</p>
     *
     * @param sizeBytes size to check
     * @throws UploadRejectedException when the content is empty
     */
    public void ensureNotEmpty(long sizeBytes) {
        if (sizeBytes == 0) {
            throw new UploadRejectedException("Cannot upload an empty file.");
        }
    }

    /**
     * 🏷️ Check the declared content type.
     *
     * <p>Content that declares no type at all is not judged on its type — there is nothing to
     * judge. The extension check still applies to it.</p>
     *
     * @param baseType bare {@code type/subtype}, or {@code null} when nothing was declared
     */
    private void ensureContentTypeAccepted(String baseType) {
        if (baseType == null) {
            return;
        }

        if (isRefused(contentTypes, baseType)) {
            throw new UploadRejectedException(
                    explain("File type '%s' is not accepted here.".formatted(baseType), contentTypes,
                            "types"));
        }
    }

    /**
     * 📄 Check the extension.
     *
     * <p>Under a denylist, content with no extension is not on the list and so passes. Under an
     * allowlist it is likewise not on the list, and so is refused — which is the point of an
     * allowlist.</p>
     *
     * @param extension lower-cased extension without its dot, possibly empty
     */
    private void ensureExtensionAccepted(String extension) {
        if (extension.isEmpty() && mode == AcceptanceMode.DENY_LIST) {
            return;
        }

        if (isRefused(extensions, extension)) {
            throw new UploadRejectedException(explain(
                    extension.isEmpty()
                            ? "A file extension is required."
                            : "Files ending '.%s' are not accepted here.".formatted(extension),
                    extensions, "extensions"));
        }
    }

    /**
     * 🗣️ A refusal, in the order somebody reads one: what is wrong, why here, and what would work.
     *
     * <p>⚠️ Each part is appended only where it exists, so an installation-wide policy with no reason
     * and a denylist naming nothing still produces one clean sentence rather than a sentence with holes
     * in it.</p>
     *
     * @param problem what the content did wrong
     * @param listed  the list the judgement was made against
     * @param plural  what that list holds, for the summary — {@code extensions}, {@code types}
     * @return the whole refusal
     */
    private String explain(String problem, Set<String> listed, String plural) {
        String summary = summarise(listed, plural);

        return summary.isEmpty() ? withReason(problem) : withReason(problem) + " " + summary;
    }

    /**
     * 🗣️ The problem, followed by why this destination is narrow, where anybody said.
     *
     * @param problem what the content did wrong
     * @return the problem, and the reason after it where there is one
     */
    private String withReason(String problem) {
        return reason == null ? problem : problem + " " + reason;
    }

    /**
     * 📋 What would have worked, in a few words.
     *
     * <p>Both modes are worth saying and they say opposite things — an allowlist names the way in, a
     * denylist names the way out — so the sentence is built from the mode rather than assuming one.
     * An empty list under a denylist refuses nothing on that axis and therefore explains nothing, which
     * is why it answers with nothing at all.</p>
     *
     * @param listed the configured list
     * @param plural what it holds
     * @return the summary, or an empty string where there is nothing useful to say
     */
    private String summarise(Set<String> listed, String plural) {
        if (listed.isEmpty()) {
            return mode == AcceptanceMode.ALLOW_LIST
                    ? "Nothing at all is accepted here."
                    : "";
        }

        List<String> sorted  = listed.stream().sorted().toList();
        String       named   = String.join(", ", sorted.subList(0, Math.min(NAMED_IN_A_REFUSAL, sorted.size())));
        String       counted = sorted.size() > NAMED_IN_A_REFUSAL
                ? "%s and %d more".formatted(named, sorted.size() - NAMED_IN_A_REFUSAL)
                : named;

        return mode == AcceptanceMode.ALLOW_LIST
                ? "Accepted %s here: %s.".formatted(plural, counted)
                : "Refused %s here: %s.".formatted(plural, counted);
    }

    /**
     * 🔎 How the lists are read.
     *
     * <p>These four accessors exist so a screen can <strong>show</strong> the rule that applies rather
     * than only discover it by being refused. A policy that can judge but not describe itself forces
     * every interface to reconstruct it from configuration, which is the same rule written twice.</p>
     *
     * @return the acceptance mode
     */
    public AcceptanceMode mode() {
        return mode;
    }

    /**
     * 🔎 The listed content types, lower-cased and without parameters.
     *
     * @return the content types
     */
    public Set<String> contentTypes() {
        return contentTypes;
    }

    /**
     * 🔎 The listed extensions, lower-cased and without their dot.
     *
     * @return the extensions
     */
    public Set<String> extensions() {
        return extensions;
    }

    /**
     * 🔎 The largest content this policy accepts.
     *
     * @return the limit, in bytes
     */
    public long maxSizeBytes() {
        return maxSizeBytes;
    }

    /**
     * 🔎 Why this destination is narrow, where anybody wrote it down.
     *
     * <p>⚠️ Prose, in whoever configured it's own words and own language. Nothing here parses it, and a
     * screen shows it as written rather than composing a sentence around it.</p>
     *
     * @return the reason, or {@code null} where none was given
     */
    public String reason() {
        return reason;
    }

    /**
     * 🚦 Read a list according to the active mode.
     *
     * @param listed    the configured list
     * @param candidate the value under judgement
     * @return {@code true} when the candidate must be refused
     */
    private boolean isRefused(Set<String> listed, String candidate) {
        if (mode == AcceptanceMode.ALLOW_LIST) {
            return !listed.contains(candidate);
        }

        return listed.contains(candidate);
    }

    private static Set<String> lowerCased(Set<String> values) {
        if (values == null) {
            return Set.of();
        }

        return values.stream().map(value -> value.toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
    }
}
