package svit.beans.example.ints;

import svit.beans.BeanScope;
import svit.beans.annotation.Configuration;
import svit.beans.annotation.Provide;

import java.util.Random;

@Configuration(name = "ints")
public class Numbers {

    @Provide
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
