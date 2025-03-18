package org.jmouse.testing_ground.templates;

import org.jmouse.core.bind.PropertyPath;

public class PathCheck {

    public static void main(String[] args) {
        PropertyPath path = PropertyPath.forPath("abc.xxx[0][1].name.last");
        System.out.println(path.sub(1));
        System.out.println(path.sup(1));

        System.out.println(path.entries().slice(0, 2));
        System.out.println(path.entries().slice(2, 4));

        System.out.println(path.entries().limit(1));
        System.out.println(path.tail());
        System.out.println(path.head());


    }

}
