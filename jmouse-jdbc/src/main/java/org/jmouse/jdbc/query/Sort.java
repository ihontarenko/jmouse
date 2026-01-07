package org.jmouse.jdbc.query;

import org.jmouse.core.Contract;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Sort {

    private final List<Order> orders;

    private Sort(List<Order> orders) {
        this.orders = List.copyOf(orders);
    }

    public static Sort unsorted() {
        return new Sort(List.of());
    }

    public static Sort by(String property) {
        return new Sort(List.of(Order.asc(property)));
    }

    public static Sort by(Order... orders) {
        List<Order> list = new ArrayList<>();

        if (orders != null) {
            for (Order order : orders) {
                if (order != null) {
                    list.add(order);
                }
            }
        }

        return new Sort(list);
    }

    public boolean isEmpty() {
        return orders.isEmpty();
    }

    public List<Order> orders() {
        return Collections.unmodifiableList(orders);
    }

    public enum Direction { ASC, DESC }

    public record Order(String property, Direction direction) {

        public Order {
            Contract.nonNull(property, "property");
            Contract.nonNull(direction, "direction");
        }

        public static Order asc(String property) {
            return new Order(property, Direction.ASC);
        }

        public static Order desc(String property) {
            return new Order(property, Direction.DESC);
        }

    }
}
