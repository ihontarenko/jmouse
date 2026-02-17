package org.jmouse.dom.building;

import org.jmouse.core.access.AccessorWrapper;
import org.jmouse.core.access.ObjectAccessor;
import org.jmouse.core.access.ObjectAccessorWrapper;
import org.jmouse.core.access.accessor.ScalarValueAccessor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ObjectAccessorExample {

    private static final AccessorWrapper WRAPPER = new ObjectAccessorWrapper();

    public static void main(String[] args) {
        // datum
        List<String> list = List.of("a", "b", "c");
        Map<String, String> map = new HashMap<>();
        map.put("a", "b");
        map.put("b", "c");
        map.put("c", "d");
        UserBean userBean = new UserBean();
        userBean.setName("John");
        userBean.setAge(25);
        UserRecord userRecord = new UserRecord("3344", "Jane");

        // accessing

        ObjectAccessor listAccessor = WRAPPER.wrap(list);
        ObjectAccessor mapAccessor = WRAPPER.wrap(map);
        ObjectAccessor userAccessor = WRAPPER.wrap(userBean);
        ObjectAccessor userRecordAccessor = WRAPPER.wrap(userRecord);

        System.out.println(
                listAccessor.keySet() //  [0, 1, 2]
        );

        System.out.println(
                mapAccessor.keySet() // [a, b, c]
        );

        System.out.println(
                userAccessor.keySet() // [name, age]
        );

        System.out.println(
                userRecordAccessor.keySet() // [name, id]
        );

        listAccessor.set(1, 22); // exception because List.of is immutable

        mapAccessor.set("d", "x");

        userAccessor.set("name", "Keanu");

        userBean.getName(); // Keanu

        ScalarValueAccessor scalarValueAccessor = (ScalarValueAccessor) userRecordAccessor.get("name");
        scalarValueAccessor.unwrap(); // Jane

        scalarValueAccessor.isScalar(); //true
        // listAccessor.isCollection() || listAccessor.isList(); // true

        mapAccessor.isMap(); // true

        userAccessor.isBean(); // true
        userRecordAccessor.isRecord(); // true

        userAccessor.isSimple(); // false

    }

    public record UserRecord(String id, String name) {}

    public static class UserBean {

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
    }


}
