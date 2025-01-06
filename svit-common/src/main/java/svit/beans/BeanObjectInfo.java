package svit.beans;

import java.beans.PropertyDescriptor;
import java.util.Map;

public interface BeanObjectInfo {

    <T> T getObject();

    Class<?> getClassType();

    Map<String, BeanField> getBeanFields();

    BeanField getBeanField(String fieldName);

    PropertyDescriptor getPropertyDescriptor(String name);

    Map<String, PropertyDescriptor> getPropertyDescriptors();

}
