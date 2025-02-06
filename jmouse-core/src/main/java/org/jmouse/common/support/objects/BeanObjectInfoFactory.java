package org.jmouse.common.support.objects;

import java.util.Map;

final public class BeanObjectInfoFactory {

    public static BeanObjectInfo createBeanObjectInfo(Object object) {
        BeanObjectInfo beanObjectInfo;

        if (object instanceof Map<?,?> map) {
            beanObjectInfo = new MapObjectBeanInfo((Map<String, Object>) map);
        } else if (object == null) {
            beanObjectInfo = new NullObjectBeanInfo();
        } else {
            beanObjectInfo = new ObjectBeanInfo(object);
        }

        return beanObjectInfo;
    }

}
