package org.jmouse.context.binding;

import java.util.List;
import java.util.Map;

public class StandartDataSource implements DataSource {

    private final Object source;

    public StandartDataSource(Object source) {
        this.source = source;
    }

    @Override
    public DataSource get(String name) {
        Object value = source;

        if (isMap()) {
            value = as(Map.class).get(name);
        }

        return new StandartDataSource(value);
    }

    @Override
    public DataSource get(int index) {
        Object value = source;

        if (is(List.class)) {
            value = as(List.class).get(index);
        }

        return new StandartDataSource(value);
    }

    @Override
    public Object getSource() {
        return source;
    }

}
