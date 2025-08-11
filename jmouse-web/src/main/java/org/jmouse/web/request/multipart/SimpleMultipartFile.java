package org.jmouse.web.request.multipart;

import jakarta.servlet.http.Part;

public record SimpleMultipartFile(Disposition disposition, Part servletPart) implements MultipartFile {

}
