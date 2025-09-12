package org.jmouse.web.mvc.resource;

import org.jmouse.context.BeanProperties;

@BeanProperties("jmouse.resource.version")
public class ResourceVersionProperties {

    private String      iconPrefix;
    private ContentHash contentHash;

    public String getIconPrefix() {
        return iconPrefix;
    }

    public void setIconPrefix(String iconPrefix) {
        this.iconPrefix = iconPrefix;
    }

    public ContentHash getContentHash() {
        return contentHash;
    }

    public void setContentHash(ContentHash contentHash) {
        this.contentHash = contentHash;
    }

    public static class ContentHash {

        private int length;

        public int getLength() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
        }
    }

}
