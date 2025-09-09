package org.jmouse.web.http.request.multipart;

import jakarta.servlet.http.Part;
import org.jmouse.core.MediaType;
import org.jmouse.web.http.request.ContentDisposition;

import java.io.IOException;
import java.io.InputStream;

/**
 * 📦 Represents an uploaded multipart file.
 *
 * <p>Provides access to file metadata ({@link ContentDisposition}), the underlying servlet {@link Part},
 * and helper methods to read file content in different forms.</p>
 *
 * @author Ivan
 */
public interface MultipartFile {

    /**
     * 🏷 Get the {@link ContentDisposition} describing this file.
     *
     * @return file disposition metadata
     */
    ContentDisposition disposition();

    /**
     * ⚙ Get the raw servlet {@link Part} for advanced control.
     *
     * @return the servlet part backing this file
     */
    Part servletPart();

    /**
     * 🎨 Get file's media type.
     *
     * @return parsed {@link MediaType}, never {@code null}
     */
    default MediaType getContentType() {
        return MediaType.forString(servletPart().getContentType());
    }

    /**
     * 📏 Get the size of the file in bytes.
     *
     * @return file size
     */
    default long getSize() {
        return servletPart().getSize();
    }

    /**
     * 🗂 Get the original file name.
     *
     * @return file name as sent by the client
     */
    default String getFileName() {
        return disposition().name();
    }

    /**
     * 📥 Open the file's content stream.
     *
     * @return {@link InputStream} for reading file content
     * @throws IOException if reading fails
     */
    default InputStream getInputStream() throws IOException {
        return servletPart().getInputStream();
    }

    /**
     * 🧾 Read all file content into a byte array.
     *
     * @return file content as bytes
     * @throws IOException if reading fails
     */
    default byte[] getBytes() throws IOException {
        return getInputStream().readAllBytes();
    }
}
