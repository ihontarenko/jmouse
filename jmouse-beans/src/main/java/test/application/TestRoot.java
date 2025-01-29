package test.application;

import svit.beans.annotation.Provide;

@Provide(proxied = true, value = "root")
public class TestRoot implements GetValue{

    public String getValue() {
        return getClass().getName();
    }

}
