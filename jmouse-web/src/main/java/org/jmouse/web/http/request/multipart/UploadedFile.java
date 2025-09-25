package org.jmouse.web.http.request.multipart;

import jakarta.servlet.http.Part;
import org.jmouse.web.http.request.ContentDisposition;

/**
 * 📎 Simple implementation of {@link MultipartFile} backed by a servlet {@link Part}.
 *
 * <p>Holds:</p>
 * <ul>
 *   <li>📑 {@link ContentDisposition} with metadata (form field name, filename, etc.)</li>
 *   <li>📂 Underlying servlet {@link Part} for accessing content and headers</li>
 * </ul>
 */
public record UploadedFile(ContentDisposition disposition, Part servletPart) implements MultipartFile {
}
