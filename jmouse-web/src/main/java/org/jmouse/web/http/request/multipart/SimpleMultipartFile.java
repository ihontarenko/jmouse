package org.jmouse.web.http.request.multipart;

import jakarta.servlet.http.Part;

public record SimpleMultipartFile(Disposition disposition, Part servletPart) implements MultipartFile {

}
