package test.application;

import org.jmouse.beans.BeanScope;
import org.jmouse.beans.annotation.Configuration;
import org.jmouse.beans.annotation.Provide;
import org.jmouse.beans.annotation.Qualifier;
import org.jmouse.util.helper.Strings;

import java.util.List;
import java.util.Locale;

@Configuration
public class BeansConfiguration {

    @Provide("defaultUserName")
    public String userName() {
        return "Chuck Norris";
    }

    @Provide(value = "gal", scope = BeanScope.PROTOTYPE)
    public String galGadot() {
        return "Gal Gadot";
    }

    @Provide
    public String upper(@Qualifier("gal") String name, Double number) {
        return "[" + Strings.underscored(name).toUpperCase(Locale.ROOT) + " / Random: " + number + "]";
    }

    @Provide
    public List<String> getNames() {
        return List.of("test");
    }

    @Provide
    public List<Integer> getYears() {
        return List.of(2024, 2025);
    }

}
