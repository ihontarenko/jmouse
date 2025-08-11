package org.jmouse.web.request.multipart;

import jakarta.servlet.http.HttpServletRequest;
import org.jmouse.web.request.WebHttpRequest;

import java.util.Collections;
import java.util.List;
import java.util.Map;

abstract public class AbstractMultipartWebHttpRequest extends WebHttpRequest implements MultipartWebRequest {

    protected Map<String, List<MultipartFile>> multipartFiles;

    public AbstractMultipartWebHttpRequest(HttpServletRequest request) {
        super(request);
    }

    public Map<String, List<MultipartFile>> getMultipartFiles() {
        return multipartFiles;
    }

    public void setMultipartFiles(Map<String, List<MultipartFile>> multipartFiles) {
        this.multipartFiles = multipartFiles;
    }

    @Override
    public List<MultipartFile> getFiles(String name) {
        return getMultipartFiles().getOrDefault(name, Collections.emptyList());
    }

    @Override
    public String getFileContentType(String name) {
        return getFirstFile(name).servletPart().getContentType();
    }
}
