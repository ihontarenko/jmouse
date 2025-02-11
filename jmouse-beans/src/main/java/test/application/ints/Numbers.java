package test.application.ints;

import org.jmouse.testing_ground.beans.BeanScope;
import org.jmouse.testing_ground.beans.annotation.Configuration;
import org.jmouse.testing_ground.beans.annotation.Provide;

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
