package org.jmouse.web.http.request.multipart;

import jakarta.servlet.http.Part;

public record SimpleMultipartFile(ContentDisposition disposition, Part servletPart) implements MultipartFile {

}
