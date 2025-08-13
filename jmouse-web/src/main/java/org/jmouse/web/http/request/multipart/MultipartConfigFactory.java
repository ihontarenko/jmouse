package org.jmouse.web.http.request.multipart;

import jakarta.servlet.MultipartConfigElement;

final public class MultipartConfigFactory {

    private long   fileSizeThreshold;
    private long   maxFileSize;
    private long   maxRequestSize;
    private String location;

    public long getFileSizeThreshold() {
        return fileSizeThreshold;
    }

    public void setFileSizeThreshold(long fileSizeThreshold) {
        this.fileSizeThreshold = fileSizeThreshold;
    }

    public long getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public long getMaxRequestSize() {
        return maxRequestSize;
    }

    public void setMaxRequestSize(long maxRequestSize) {
        this.maxRequestSize = maxRequestSize;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public MultipartConfigElement createMultipartConfig() {
        return new MultipartConfigElement(
                getLocation(), getMaxFileSize(), getMaxRequestSize(), (int) getFileSizeThreshold());
    }

}
