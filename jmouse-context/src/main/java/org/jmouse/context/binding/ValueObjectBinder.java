package org.jmouse.context.binding;

import org.jmouse.context.binding.bean.Bean;
import org.jmouse.context.binding.bean.ValueObject;
import org.jmouse.core.reflection.TypeDescriptor;
import org.jmouse.util.Priority;

import java.util.Map;
import java.util.function.Supplier;

@Priority(Integer.MIN_VALUE / 2)
public class ValueObjectBinder extends AbstractBinder {

    public ValueObjectBinder(BindContext context) {
        super(context);
    }

    @Override
    @SuppressWarnings({"unchecked"})
    public <T> BindingResult<T> bind(NamePath name, Bindable<T> bindable, DataSource source) {
        Class<?> rawType = bindable.getType().getRawType();

        if (bindable.getTypeDescriptor().isRecord()) {
            ValueObject<?>     vo      = ValueObject.of((Class<? extends Record>) rawType);
            ValueObject.Values values  = vo.getRecordValues();
            Supplier<?>        factory = vo.getInstance(values);

            for (Bean.Property<?> property : vo.getProperties()) {
                String                                   propertyName = property.getName();
                Bean.Setter<Map<String, Object>, Object> setter       = Bean.Setter.ofMap(propertyName);
                Bindable<?>                              component    = Bindable.of(property.getType());

                bindValue(name.append(propertyName), component, source).ifPresent(bound -> setter.set(values, bound));
            }

            return BindingResult.of((T) factory.get());
        }

        return BindingResult.empty();
    }

    @Override
    public <T> boolean supports(Bindable<T> bindable) {
        return TypeDescriptor.forJavaType(bindable.getType()).isRecord();
    }

}
