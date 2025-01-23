package svit.support.objects;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static java.util.Optional.ofNullable;

public class ObjectBeanInfo extends AbstractBeanInfo {

    public ObjectBeanInfo(Object object) {
        super(object);
        this.fields = createFields();
    }

    public Map<String, BeanField> createFields() {
        Map<String, BeanField> fields = new HashMap<>();

        for (Field field : classType.getDeclaredFields()) {
            field.setAccessible(true);
            BeanField beanField = new ObjectBeanField(field, object);
            fields.put(beanField.getName(), beanField);
        }

        return fields;
    }

    @Override
    public PropertyDescriptor getPropertyDescriptor(String name) {
        return ofNullable(descriptors.get(name)).orElseThrow(() -> new BeanPropertyDescriptorNotFoundException(
                "UNABLE TO FIND PROPERTY DESCRIPTOR '%s'".formatted(name)));
    }
}
