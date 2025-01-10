package svit.container.example.ints;

import svit.container.annotation.Configuration;
import svit.container.annotation.Provide;

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

}
