package org.jmouse.context.binding;

import org.jmouse.core.reflection.JavaType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Example {

    public static void main(String[] args) {

        Map<String, Object> source = new HashMap<>() {{
            put("key", "value");
            put("key2", "value2");
            put("list", List.of(1, 2));
            put("map", Map.of("key", "value", "key2", "value2"));
            put("array", List.of(1, 2, 3).toArray());
            put("boolean", Boolean.TRUE);
            put("byte", Byte.valueOf((byte) 1));
            put("char", Character.valueOf((char) 1));
            put("short", Short.valueOf((short) 1));
            put("int", Integer.valueOf(1));
            put("long", Long.valueOf(1));
            put("float", Float.valueOf(1));
            put("double", Double.valueOf(1));
            put("object", new User());
        }};

        DataSource dataSource = new StandartDataSource(source);

        System.out.println(dataSource.asMap(String.class, Object.class).get("array"));
        System.out.println(dataSource.get("array"));
        System.out.println(dataSource.get("list").as(List.class).get(1));

        Binder binder = new Binder(new StandartDataSource(new HashMap<>() {{
            put("name", "John");
            put("age", 13);
        }}));

        binder.bind("list", Bindable.of(User.class));

//        PropertyName.Elements elements = PropertyName.of("a.b.c.d.e.f").getElements();
//
//        System.out.println(elements.toOriginal());
//        System.out.println(elements.slice(1, 3));
//        System.out.println(elements.limit(2));
//        System.out.println(elements.skip(4));

//        Bean<User> bean = Bean.of(User.class);
//        BindableBean<User> bindable = BindableBean.of(User.class);
//        Supplier<User> supplier = bean.getSupplier(bindable);
//
//        for (Bean.Property property : bean.getProperties()) {
//            System.out.println(property.getType());
//            property.setValue(supplier, "Hello World");
//        }
//
//        System.out.println(supplier.get());
    }

    static public class User {
        private String name;
        private int age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        @Override
        public String toString() {
            return "User{name='%s', age=%d}".formatted(name, age);
        }

    }

}
