package org.jmouse.storage.configuration;

import org.jmouse.core.binding.BindDefault;
import org.jmouse.storage.policy.AcceptanceMode;

import java.util.HashSet;
import java.util.Set;

/**
 * 🚦 What may enter storage.
 *
 * <p>The lists mean the opposite of each other depending on {@link #mode}, which is what makes one
 * component serve both a product that blocks dangerous formats and a product that admits only a
 * closed set. Content type and extension are listed separately, and checked separately, because a
 * client controls both and may lie about either.</p>
 *
 * <p>An empty list under {@link AcceptanceMode#ALLOW_LIST} admits nothing, which is the honest
 * reading of "accept only these" and makes a half-written allowlist fail loudly rather than
 * silently open the gate. Under {@link AcceptanceMode#DENY_LIST} an empty list refuses nothing.</p>
 *
 * <p>{@link #blockingDangerousContent()} and {@link #allowingDocumentsAndImages()} ship the two
 * configurations already in production, so adopting the library does not mean re-typing sixty
 * extensions into a YAML file and getting one of them wrong.</p>
 *
 * <h3>Choosing one</h3>
 *
 * <p>Set {@link #profile} and you are done — the two profiles are the configurations already in
 * production, and picking one is a single line rather than sixty extensions retyped into a YAML
 * file with one of them wrong:</p>
 *
 * <pre>{@code
 * innoventa.file.upload.profile: BLOCK_DANGEROUS_CONTENT
 * moneta.file.upload.profile: ALLOW_DOCUMENTS_AND_IMAGES
 * }</pre>
 *
 * <p>{@link UploadProfile#CUSTOM} reads {@link #mode}, {@link #contentTypes} and
 * {@link #extensions} instead, for a product whose answer is genuinely its own.</p>
 *
 * <h3>A profile, plus one more thing</h3>
 *
 * <p>The lists may be written <em>beside</em> a named profile, and then they extend it in its own
 * direction — see {@link #resolve()}. A media hub that keeps films wants the shipped denylist and one
 * addition, and that is a line rather than a fork of the list:</p>
 *
 * <pre>{@code
 * jmouse.storage.upload.profile: BLOCK_DANGEROUS_CONTENT
 * jmouse.storage.upload.extensions: [apk]
 * jmouse.storage.upload.content-types: [application/vnd.android.package-archive]
 * }</pre>
 *
 * @param profile      a shipped configuration, or {@link UploadProfile#CUSTOM} to spell one out
 * @param mode         how {@link #contentTypes} and {@link #extensions} are read; ⚠️ a named profile
 *                     keeps its own direction and this is not consulted
 * @param contentTypes bare {@code type/subtype} values, without parameters — the whole list under
 *                     {@code CUSTOM}, additions to the profile otherwise
 * @param extensions   extensions without their dot, read the same way
 */
public record UploadSettings(@BindDefault("CUSTOM") UploadProfile profile,
                             @BindDefault("DENY_LIST") AcceptanceMode mode,
                             Set<String> contentTypes,
                             Set<String> extensions) {

    /**
     * 🏗️ Fill in whatever configuration omitted, so an absent block means "accept anything" rather
     * than a null dereference on first upload.
     */
    public UploadSettings {
        profile      = (profile == null) ? UploadProfile.CUSTOM : profile;
        mode         = (mode == null) ? AcceptanceMode.DENY_LIST : mode;
        contentTypes = (contentTypes == null) ? Set.of() : Set.copyOf(contentTypes);
        extensions   = (extensions == null) ? Set.of() : Set.copyOf(extensions);
    }

    /**
     * ✅ These settings as the policy actually applies them: a named profile when one was chosen,
     * otherwise exactly what was configured.
     *
     * @return the effective settings
     */
    public UploadSettings resolve() {
        return switch (profile) {
            case BLOCK_DANGEROUS_CONTENT -> extending(blockingDangerousContent());
            case ALLOW_DOCUMENTS_AND_IMAGES -> extending(allowingDocumentsAndImages());
            case ALLOW_DOCUMENTS_IMAGES_AND_TEXT -> extending(allowingDocumentsImagesAndText());
            case ALLOW_MEDIA_DOCUMENTS_AND_IMAGES -> extending(allowingMediaDocumentsAndImages());
            case CUSTOM -> this;
        };
    }

    /**
     * ➕ A shipped profile with this product's own entries folded into it.
     *
     * <h3>⚠️ The lists beside a profile EXTEND it; they used to be discarded in silence</h3>
     *
     * <p>"The shipped list, plus this one thing" is the commonest real requirement a product has, and
     * it was the one thing this record could not express: a named profile returned its own lists and
     * ignored everything configured beside them. A product needing one more extension had to choose
     * between copying sixty of them into its YAML — a copy that goes stale the day the library
     * corrects its own — and declaring a policy bean in code to get round its own configuration.</p>
     *
     * <p>⚠️ <strong>In the profile's own direction, and the profile keeps the direction.</strong> Added
     * to a denylist an entry is refused as well; added to an allowlist it is admitted as well. A
     * {@link #mode} written beside a profile is not honoured, because "an allowlist profile, read as a
     * denylist" is not a stricter rule or a looser one — it is a different rule with the profile's
     * name on it.</p>
     *
     * @param shipped the profile as it ships
     * @return the profile itself where nothing was added, otherwise the union
     */
    private UploadSettings extending(UploadSettings shipped) {
        if (contentTypes.isEmpty() && extensions.isEmpty()) {
            return shipped;
        }

        Set<String> allTypes = new HashSet<>(shipped.contentTypes());
        allTypes.addAll(contentTypes);

        Set<String> allExtensions = new HashSet<>(shipped.extensions());
        allExtensions.addAll(extensions);

        /*
          ⚠️ CUSTOM on the way out, so resolving is idempotent. Keeping the profile's name on a set
          that is no longer the profile's would make a second resolve throw the additions away again —
          and something somewhere always resolves twice.
         */
        return new UploadSettings(UploadProfile.CUSTOM, shipped.mode(), allTypes, allExtensions);
    }

    /**
     * 🕊️ Settings that refuse nothing.
     *
     * @return a permissive denylist
     */
    public static UploadSettings permissive() {
        return new UploadSettings(UploadProfile.CUSTOM, AcceptanceMode.DENY_LIST, Set.of(), Set.of());
    }

    /**
     * ⛔ Refuse anything that executes: native binaries, shell and server-side scripts, Java
     * archives, and markup.
     *
     * <p>For a product accepting arbitrary user files, where enumerating what is safe is not
     * possible. SVG is on both lists: it is an image by every intuition and a script host by
     * specification, and an SVG served from a product's own origin runs against the session of
     * whoever opens it.</p>
     *
     * @return a denylist of executable and scriptable formats
     */
    public static UploadSettings blockingDangerousContent() {
        Set<String> contentTypes = Set.of(
                // native executables
                "application/x-msdownload", "application/x-executable",
                "application/x-dosexec", "application/x-msdos-program",
                "application/vnd.microsoft.portable-executable",
                // shell and batch scripts
                "application/x-sh", "text/x-sh", "application/x-shellscript", "text/x-shellscript",
                "application/x-bat", "application/x-msdos-batch", "text/x-msdos-batch",
                // server-side scripts
                "application/x-php", "text/x-php", "application/x-httpd-php",
                "application/x-httpd-php-source",
                "text/x-python", "application/x-python", "application/x-python-code",
                "application/x-perl", "text/x-perl",
                "application/x-ruby", "text/x-ruby",
                // markup and script, the cross-site-scripting vectors
                "text/html", "application/xhtml+xml", "image/svg+xml",
                "application/javascript", "text/javascript", "application/x-javascript"
        );

        Set<String> extensions = Set.of(
                // windows executables and scripts
                "exe", "bat", "cmd", "com", "pif", "scr", "vbs", "vbe", "jse", "wsf", "wsh",
                "msi", "dll", "sys", "drv", "ps1", "ps2", "psm1", "psc1",
                // unix scripts
                "sh", "bash", "zsh", "fish", "csh", "ksh",
                // server-side scripts
                "php", "php3", "php4", "php5", "php7", "php8", "phtml", "phps",
                "py", "pyc", "pyo", "rb", "pl", "cgi",
                // java archives, which execute
                "jar", "war", "ear",
                // markup and script
                "html", "htm", "xhtml", "shtml", "svg",
                "htaccess", "htpasswd",
                // shortcuts that resolve elsewhere
                "lnk", "url", "desktop"
        );

        return new UploadSettings(UploadProfile.CUSTOM, AcceptanceMode.DENY_LIST, contentTypes, extensions);
    }

    /**
     * ✅ Admit only images, PDF and the common Office formats.
     *
     * <p>For a product whose supported formats are a closed set — where anything unrecognised is
     * a mistake rather than a file someone meant to keep.</p>
     *
     * @return an allowlist of document and image formats
     */
    public static UploadSettings allowingDocumentsAndImages() {
        Set<String> contentTypes = Set.of(
                "image/jpeg", "image/png", "image/gif", "image/webp", "image/bmp", "image/heic",
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        );

        Set<String> extensions = Set.of(
                "jpg", "jpeg", "png", "gif", "webp", "bmp", "heic",
                "pdf", "doc", "docx", "xls", "xlsx"
        );

        return new UploadSettings(UploadProfile.CUSTOM, AcceptanceMode.ALLOW_LIST, contentTypes, extensions);
    }

    /**
     * ✅ Documents, images, and the <strong>inert text</strong> formats.
     *
     * <p>{@link #allowingDocumentsAndImages()} plus notes, logs, exports and configuration — the files a
     * tracker or a knowledge base is handed most often after a screenshot, and which that profile
     * refuses outright.</p>
     *
     * <p>⚠️ <strong>INERT is the load-bearing word.</strong> Absent by name and not by oversight:
     * {@code text/html}, {@code application/xhtml+xml}, {@code image/svg+xml} and every JavaScript
     * type. They are text by encoding and a script host by specification, and a product that serves
     * uploaded bytes from its own origin — a public avatar route, a public file route — is safe
     * precisely because they are not in a list like this one.</p>
     *
     * <p>⚠️ {@code text/xml} is out too, for the same reason one step removed: an XML document can carry
     * a stylesheet that executes, and "it is only data" is a claim about the file rather than about the
     * format.</p>
     *
     * @return an allowlist of document, image and inert text formats
     */
    public static UploadSettings allowingDocumentsImagesAndText() {
        Set<String> contentTypes = new HashSet<>(allowingDocumentsAndImages().contentTypes());

        contentTypes.addAll(Set.of(
                "text/plain", "text/markdown", "text/x-markdown", "text/csv", "text/tab-separated-values",
                "application/json", "application/x-ndjson",
                "application/yaml", "application/x-yaml", "text/yaml", "text/x-yaml",
                "text/x-log"
        ));

        Set<String> extensions = new HashSet<>(allowingDocumentsAndImages().extensions());

        extensions.addAll(Set.of(
                "txt", "md", "markdown", "csv", "tsv", "json", "ndjson", "yaml", "yml", "log"
        ));

        return new UploadSettings(UploadProfile.CUSTOM, AcceptanceMode.ALLOW_LIST,
                                  Set.copyOf(contentTypes), Set.copyOf(extensions));
    }

    /**
     * ✅ Films, music and their subtitles, on top of {@link #allowingDocumentsImagesAndText()}.
     *
     * <p>For a product whose cabinet holds <strong>media</strong>: a household's film library, a photo
     * archive, anything where an {@code .mkv} is the ordinary case rather than a surprise. The two
     * document profiles refuse every video and every audio file, so a media product choosing one of
     * them refuses the files it exists for — and choosing {@link #blockingDangerousContent()} instead
     * swings the other way and accepts anything nobody thought to name.</p>
     *
     * <h3>⚠️ AN ALLOW-LIST, so an installer is refused by NOT BEING ON IT</h3>
     *
     * <p>Which is the point: {@code .apk}, {@code .dmg}, {@code .deb}, and whatever ships next, are all
     * refused without anybody keeping a list of them up to date. A product that genuinely wants one of
     * them somewhere puts a rule on the <em>one folder</em> that takes it, which is a sentence a person
     * can read off a screen.</p>
     *
     * <h3>⚠️ CONTAINERS BY EXTENSION AND BY TYPE, because browsers disagree about media types</h3>
     *
     * <p>The same {@code .mkv} arrives as {@code video/x-matroska}, {@code video/mkv} or
     * {@code application/octet-stream} depending on the operating system — and both halves are checked
     * independently, so a file refused on its declared type while its extension was listed would be a
     * refusal nobody could act on. The types below are the ones actually seen; the extension list is
     * what carries the rest.</p>
     *
     * @return an allowlist of media, document, image and inert text formats
     */
    public static UploadSettings allowingMediaDocumentsAndImages() {
        Set<String> contentTypes = new HashSet<>(allowingDocumentsImagesAndText().contentTypes());

        contentTypes.addAll(Set.of(
                // video containers
                "video/mp4", "video/x-matroska", "video/quicktime", "video/x-msvideo", "video/webm",
                "video/mpeg", "video/mp2t", "video/x-ms-wmv", "video/ogg", "video/3gpp",
                // audio
                "audio/mpeg", "audio/mp4", "audio/aac", "audio/flac", "audio/x-flac", "audio/ogg",
                "audio/opus", "audio/wav", "audio/x-wav", "audio/x-ms-wma",
                // subtitles, which travel with a film
                "text/vtt", "application/x-subrip",
                /*
                  ⚠️ The honest one. A large file dragged from a file manager frequently arrives with no
                  usable type at all, and refusing it on that would refuse films that are perfectly
                  ordinary. The EXTENSION still has to be on the list below, which is where the decision
                  actually lives for media.
                 */
                "application/octet-stream"
        ));

        Set<String> extensions = new HashSet<>(allowingDocumentsImagesAndText().extensions());

        extensions.addAll(Set.of(
                // video
                "mp4", "m4v", "mkv", "mov", "avi", "webm", "mpg", "mpeg", "ts", "m2ts", "wmv", "flv",
                "ogv", "3gp",
                // audio
                "mp3", "m4a", "aac", "flac", "ogg", "oga", "opus", "wav", "wma",
                // subtitles and the sidecar a media library writes beside a film
                "srt", "vtt", "ass", "ssa", "sub", "nfo"
        ));

        return new UploadSettings(UploadProfile.CUSTOM, AcceptanceMode.ALLOW_LIST,
                                  Set.copyOf(contentTypes), Set.copyOf(extensions));
    }
}
