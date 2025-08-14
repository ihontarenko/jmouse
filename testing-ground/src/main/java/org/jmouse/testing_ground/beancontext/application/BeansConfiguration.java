package org.jmouse.testing_ground.beancontext.application;

import org.jmouse.beans.BeanScope;
import org.jmouse.beans.annotation.Bean;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.beans.annotation.Qualifier;
import org.jmouse.util.Strings;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

@BeanFactories
public class BeansConfiguration {

    @Bean("defaultUserName")
    public String userName() {
        return "Chuck Norris";
    }

    @Bean(value = "gal", scope = BeanScope.PROTOTYPE)
    public String galGadot() {
        return "Gal Gadot";
    }

    @Bean
    public String upper(@Qualifier("gal") String name, Double number) {
        return "[" + Strings.underscored(name).toUpperCase(Locale.ROOT) + " / Random: " + number + "]";
    }

    @Bean
    public List<String> getNames() {
        return List.of("test");
    }

    @Bean
    public List<Integer> getYears() {
        return List.of(2024, 2025);
    }

    @Bean
    public Collection<Integer> getIntegers() {
        return List.of(1, 2, 3);
    }

    @Bean
    public Collection<Float> getFloats() {
        return List.of(1f, 2f, 3f);
    }

}
