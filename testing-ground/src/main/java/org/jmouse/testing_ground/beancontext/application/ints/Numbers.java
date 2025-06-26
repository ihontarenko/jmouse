package org.jmouse.testing_ground.beancontext.application.ints;

import org.jmouse.beans.BeanScope;
import org.jmouse.beans.annotation.BeanFactories;
import org.jmouse.beans.annotation.Bean;

import java.util.Random;

@BeanFactories(name = "ints")
public class Numbers {

    @Bean("int123")
    public Integer int123() {
        return 123;
    }

    @Bean
    public Float float123() {
        return 123F;
    }

    @Bean
    public Integer int777() {
        return 777;
    }

    @Bean(scope = BeanScope.PROTOTYPE)
    public Double doubleRandom() {
        return new Random().nextDouble();
    }

}
