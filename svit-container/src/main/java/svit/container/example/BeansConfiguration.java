package svit.container.example;

import svit.container.Lifecycle;
import svit.container.annotation.Configuration;
import svit.container.annotation.Provide;
import svit.container.annotation.Qualifier;
import svit.util.Strings;

import java.util.Locale;

@Configuration
public class BeansConfiguration {

    @Provide("defaultUserName")
    public String userName() {
        return "Chuck Norris";
    }

    @Provide(value = "gal", lifecycle = Lifecycle.PROTOTYPE)
    public String galGadot() {
        return "Gal Gadot";
    }

    @Provide
    public String upper(@Qualifier("gal") String name, Float number) {
        return "[" + Strings.underscored(name).toUpperCase(Locale.ROOT) + number + "]";
    }

}
