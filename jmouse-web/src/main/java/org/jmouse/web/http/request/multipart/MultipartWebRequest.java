package org.jmouse.web.http.request.multipart;

import java.util.List;

public interface MultipartWebRequest {

    List<MultipartFile> getFiles(String name);

    default MultipartFile getFirstFile(String name) {
        return getFiles(name).getFirst();
    }

    String getFileContentType(String name);

}
