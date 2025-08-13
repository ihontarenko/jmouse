package org.jmouse.web.http.request.multipart;

import jakarta.servlet.MultipartConfigElement;
import org.jmouse.context.BeanProperties;
import org.jmouse.core.Bytes;
import org.jmouse.util.AttributeMapper;

@BeanProperties("jmouse.web.server.multipart")
public class MultipartProperties {

    private boolean enabled           = true;
    private Bytes   fileSizeThreshold = Bytes.ofBytes(0);
    private Bytes   maxFileSize       = Bytes.ofMegabytes(1);
    private Bytes   maxRequestSize    = Bytes.ofMegabytes(10);
    private String  location;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Bytes getFileSizeThreshold() {
        return fileSizeThreshold;
    }

    public void setFileSizeThreshold(Bytes fileSizeThreshold) {
        this.fileSizeThreshold = fileSizeThreshold;
    }

    public Bytes getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(Bytes maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public Bytes getMaxRequestSize() {
        return maxRequestSize;
    }

    public void setMaxRequestSize(Bytes maxRequestSize) {
        this.maxRequestSize = maxRequestSize;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public MultipartConfigElement getMultipartConfig() {
        AttributeMapper        mapper  = AttributeMapper.get();
        MultipartConfigFactory factory = new MultipartConfigFactory();

        mapper.get(this::getLocation)
                .whenNonNull()
                .accept(factory::setLocation);
        mapper.get(this::getMaxFileSize)
                .whenNonNull()
                .as(Bytes::toBytes)
                .accept(factory::setMaxFileSize);
        mapper.get(this::getFileSizeThreshold)
                .whenNonNull()
                .as(Bytes::toBytes)
                .accept(factory::setFileSizeThreshold);
        mapper.get(this::getMaxRequestSize)
                .whenNonNull()
                .as(Bytes::toBytes)
                .accept(factory::setMaxRequestSize);

        return factory.createMultipartConfig();
    }

}
