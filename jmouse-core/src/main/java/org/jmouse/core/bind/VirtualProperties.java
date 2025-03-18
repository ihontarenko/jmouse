package org.jmouse.core.bind;

public interface VirtualProperties {

    interface Aware {

        void setVirtualProperties(VirtualProperties virtualProperties);

        VirtualProperties getVirtualProperties();

    }

}
