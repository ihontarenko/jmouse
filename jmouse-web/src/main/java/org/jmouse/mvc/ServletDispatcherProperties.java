package org.jmouse.mvc;

import org.jmouse.context.BeanProperties;
import org.jmouse.core.bind.BindDefault;

@BeanProperties("jmouse.web.server.dispatcher")
public class ServletDispatcherProperties {

    private String[] mappings;
    private int      loadOnStartup;
    private boolean  enabled;

    public boolean isEnabled() {
        return enabled;
    }

    @BindDefault("true")
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String[] getMappings() {
        return mappings;
    }

    @BindDefault("/*")
    public void setMappings(String[] mappings) {
        this.mappings = mappings;
    }

    public int getLoadOnStartup() {
        return loadOnStartup;
    }

    @BindDefault("1")
    public void setLoadOnStartup(int loadOnStartup) {
        this.loadOnStartup = loadOnStartup;
    }

}
