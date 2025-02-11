package org.jmouse.testing_ground.beancontext.application.ints;

import org.jmouse.beans.BeanScope;
import org.jmouse.beans.annotation.Configuration;
import org.jmouse.beans.annotation.Provide;

import java.util.Random;

@Configuration(name = "ints")
public class Numbers {

    @Provide("int123")
    public Integer int123() {
        return 123;
    }

    @Provide
    public Float float123() {
        return 123F;
    }

    @Provide
    public Integer int777() {
        return 777;
    }

    @Provide(scope = BeanScope.PROTOTYPE)
    public Double doubleRandom() {
        return new Random().nextDouble();
    }

}
