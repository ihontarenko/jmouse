package org.jmouse.storage.configuration;

import org.jmouse.core.binding.BindDefault;
import org.jmouse.storage.policy.AcceptanceMode;

import java.util.Set;

/**
 * 🚦 What may enter storage.
 *
 * <p>The lists mean the opposite of each other depending on {@link #mode}, which is what makes one
 * component serve both a product that blocks dangerous formats and a product that admits only a
 * closed set. Content type and extension are listed separately, and checked separately, because a
 * client controls both and may lie about either.</p>
 *
 * <p>An empty list under {@link AcceptanceMode#ALLOWLIST} admits nothing, which is the honest
 * reading of "accept only these" and makes a half-written allowlist fail loudly rather than
 * silently open the gate. Under {@link AcceptanceMode#DENYLIST} an empty list refuses nothing.</p>
 *
 * <p>{@link #blockingDangerousContent()} and {@link #allowingDocumentsAndImages()} ship the two
 * configurations already in production, so adopting the library does not mean re-typing sixty
 * extensions into a YAML file and getting one of them wrong.</p>
 *
 * @param mode         how {@link #contentTypes} and {@link #extensions} are read
 * @param contentTypes bare {@code type/subtype} values, without parameters
 * @param extensions   extensions without their dot
 */
public record UploadSettings(@BindDefault("DENYLIST") AcceptanceMode mode, Set<String> contentTypes,
                             Set<String> extensions) {

    /**
     * 🏗️ Fill in whatever configuration omitted, so an absent block means "accept anything" rather
     * than a null dereference on first upload.
     */
    public UploadSettings {
        mode         = (mode == null) ? AcceptanceMode.DENYLIST : mode;
        contentTypes = (contentTypes == null) ? Set.of() : Set.copyOf(contentTypes);
        extensions   = (extensions == null) ? Set.of() : Set.copyOf(extensions);
    }

    /**
     * 🕊️ Settings that refuse nothing.
     *
     * @return a permissive denylist
     */
    public static UploadSettings permissive() {
        return new UploadSettings(AcceptanceMode.DENYLIST, Set.of(), Set.of());
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

        return new UploadSettings(AcceptanceMode.DENYLIST, contentTypes, extensions);
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

        return new UploadSettings(AcceptanceMode.ALLOWLIST, contentTypes, extensions);
    }
}
