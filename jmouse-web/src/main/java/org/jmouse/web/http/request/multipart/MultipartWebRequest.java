package org.jmouse.web.http.request.multipart;

import java.util.List;

/**
 * 📂 Abstraction for accessing multipart request data.
 *
 * <p>Provides access to uploaded files and their metadata by form field name.</p>
 */
public interface MultipartWebRequest {

    /**
     * 📑 Get all files associated with the given field name.
     *
     * @param name form field name
     * @return list of matching {@link MultipartFile}s (may be empty)
     */
    List<MultipartFile> getFiles(String name);

    /**
     * 📎 Get the first file for the given field name.
     *
     * @param name form field name
     * @return first {@link MultipartFile}, or throws if none exist
     */
    default MultipartFile getFirstFile(String name) {
        return getFiles(name).getFirst();
    }

    /**
     * 🏷️ Get the content type of the uploaded file for the given field.
     *
     * @param name form field name
     * @return content type string, or {@code null} if not available
     */
    String getFileContentType(String name);
}
