package org.jmouse.storage.delivery;

import org.jmouse.core.MediaType;
import org.jmouse.http.ContentDisposition;
import org.jmouse.storage.ContentTypes;

import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * 🎁 Decides how a browser should present a file.
 *
 * <p>In the library rather than in a product because a direct link bakes these headers into its
 * signature long before any response is built. If the two delivery paths disagree, the same file
 * downloads from an object store and renders inline from disk — and nobody notices until a user
 * reports it.</p>
 */
public final class ContentDispositions {

    /**
     * 🖼️ Show it in place.
     */
    public static final String INLINE = "inline";

    /**
     * 💾 Save it to disk.
     */
    public static final String ATTACHMENT = "attachment";

    /**
     * ☠️ Types a browser will happily execute script out of.
     *
     * <p>A public file route is typically unauthenticated and served from the same origin as the
     * application it belongs to, whose access token sits in browser storage — so anything rendered
     * inline from it runs against the session of whoever opens the link. SVG is the one that
     * surprises people: an image by every intuition, a script host by specification. Markup reaches
     * the same place, and the URL-import path may not consult the upload policy at all, so this
     * disposition is the last thing standing between a stored document and a stolen session.</p>
     *
     * <p>Deliberately narrow. This is not "images are dangerous" — every other image type still
     * renders inline, because previews are the entire point of serving a file inline.</p>
     */
    private static final Set<String> ACTIVE_CONTENT_TYPES = Set.of(
            "image/svg+xml",
            "text/html", "application/xhtml+xml",
            "text/xml", "application/xml"
    );

    private static final String IMAGE_TYPE = "image";
    private static final String TEXT_TYPE  = "text";
    private static final String PDF_TYPE   = "application/pdf";

    private ContentDispositions() {
    }

    /**
     * 🎁 Compose the disposition a file should be served with.
     *
     * <p>The UTF-8 form is requested only for filenames that actually need it. Asking for it
     * unconditionally makes a plain ASCII name carry both a legacy {@code filename=} and an
     * RFC 5987 {@code filename*=}, which is redundant and reads as a bug to anyone inspecting the
     * response.</p>
     *
     * @param contentType   type the file is served as
     * @param filename      name to present to the user
     * @param forceDownload {@code true} to save rather than show, whatever the type
     * @return the composed disposition
     */
    public static ContentDisposition compose(MediaType contentType, String filename, boolean forceDownload) {
        String type = (!forceDownload && isViewableInBrowser(contentType)) ? INLINE : ATTACHMENT;

        ContentDisposition.Builder builder = ContentDisposition.create().type(type).filename(filename);

        if (!isUsAscii(filename)) {
            builder.charset(StandardCharsets.UTF_8);
        }

        return builder.build();
    }

    /**
     * 👁️ Whether a browser can display this type without downloading it — and without running
     * script out of it.
     *
     * @param contentType the type to judge, may be {@code null}
     * @return {@code true} when the file may be shown inline
     */
    public static boolean isViewableInBrowser(MediaType contentType) {
        String baseType = ContentTypes.baseType(contentType);

        if (baseType == null || ACTIVE_CONTENT_TYPES.contains(baseType)) {
            return false;
        }

        return baseType.startsWith(IMAGE_TYPE) || baseType.startsWith(TEXT_TYPE) || baseType.equals(PDF_TYPE);
    }

    /**
     * 🔤 Whether a filename survives the legacy header form unchanged.
     *
     * @param filename name to check
     * @return {@code true} when every character is US-ASCII
     */
    private static boolean isUsAscii(String filename) {
        return StandardCharsets.US_ASCII.newEncoder().canEncode(filename);
    }
}
