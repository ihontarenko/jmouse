package org.jmouse.web.servlet.filter.properties;

import org.jmouse.beans.annotation.BeanConstructor;
import org.jmouse.beans.annotation.Provide;
import org.jmouse.core.bind.Bindable;
import org.jmouse.core.reflection.Reflections;
import org.jmouse.web.context.WebBeanContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Provide
public class FilterInitializers {

    public static final String                        JMOUSE_WEB_SERVER_FILTER_PATH = "jmouse.web.server.filter";
    private final       WebBeanContext                context;
    private             List<Class<?>> filterClasses;
    private             Map<String, Properties>                           properties;

    @BeanConstructor
    public FilterInitializers(WebBeanContext context) {
        this.context = context;
    }

    public List<Class<?>> getFilterClasses() {
        List<? extends Class<?>> filterClasses = this.filterClasses;

        if (filterClasses == null) {
            filterClasses = getProperties().keySet().stream().map(Reflections::getClassFor).toList();
            this.filterClasses = (List<Class<?>>) filterClasses;
        }

        return this.filterClasses;
    }

    public Map<String, Properties> getProperties() {
        Map<String, Properties> properties = this.properties;

        if (properties == null) {
            properties = new HashMap<>();
            Bindable<Map<String, Properties>> mapBindable = Bindable.ofMap(String.class, Properties.class);
            context.getEnvironmentBinder()
                    .bind(JMOUSE_WEB_SERVER_FILTER_PATH, mapBindable).ifPresent(properties::putAll);
            this.properties = properties;
        }

        return properties;
    }

    public record Properties(String[] mappings, boolean matchAfter) {

    }

}
