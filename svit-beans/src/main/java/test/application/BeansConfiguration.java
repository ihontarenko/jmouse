package test.application;

import svit.beans.BeanScope;
import svit.beans.annotation.Configuration;
import svit.beans.annotation.Provide;
import svit.beans.annotation.Qualifier;
import svit.util.Strings;

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

}
