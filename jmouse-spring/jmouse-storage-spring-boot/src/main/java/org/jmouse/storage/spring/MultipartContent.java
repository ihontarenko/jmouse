package org.jmouse.storage.spring;

import org.jmouse.storage.Content;
import org.jmouse.storage.exception.UploadRejectedException;
import org.springframework.web.multipart.MultipartFile;

/**
 * 📎 Turns Spring's uploaded-file type into the library's neutral content object.
 *
 * <p>The adaptation lives here, in the framework module, rather than in the library — which is the
 * point of having a framework module at all. The library takes a filename, a claimed type, a
 * claimed size and a way to open the bytes; whether those arrived as a multipart part, a remote
 * fetch, a scheduled import or a test fixture is not its problem.</p>
 *
 * <p>Everything a multipart part reports about itself is a <em>claim</em> made by the client:
 * filename, content type and size are all attacker-controlled. They are carried across as claims,
 * which is exactly how the acceptance policy and the write path treat them.</p>
 */
public final class MultipartContent {

    private static final String FALLBACK_FILENAME = "upload";

    private MultipartContent() {
    }

    /**
     * 📎 Adapt an uploaded part.
     *
     * <p>A part with no filename gets a stand-in rather than being refused here: the file may
     * genuinely be extensionless, and deciding whether that is acceptable belongs to the upload
     * policy, which reads the extension itself. Refusing in the adapter would move a policy
     * decision into a type conversion.</p>
     *
     * @param file the uploaded part
     * @return the content
     * @throws UploadRejectedException when the part carries no bytes at all
     */
    public static Content of(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new UploadRejectedException("Cannot upload an empty file.");
        }

        return Content.of(filenameOf(file), file.getContentType(), file.getSize(), file::getInputStream);
    }

    /**
     * 📄 The name the part arrived under, falling back to a stand-in.
     *
     * @param file the uploaded part
     * @return a non-blank filename
     */
    private static String filenameOf(MultipartFile file) {
        String filename = file.getOriginalFilename();
        return (filename == null || filename.isBlank()) ? FALLBACK_FILENAME : filename;
    }
}
