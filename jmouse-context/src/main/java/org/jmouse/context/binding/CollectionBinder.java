package org.jmouse.context.binding;

import org.jmouse.core.reflection.JavaType;

abstract public class CollectionBinder extends AbstractBinder {

    public CollectionBinder(BindContext context) {
        super(context);
    }

    @Override
    public <T> BindingResult<T> bindValue(NamePath name, Bindable<T> bindable, DataSource source) {
        ObjectBinder rootBinder = context.getRootBinder();
        DataSource value = source.get(name);

        if (value.isSimple()) {
            System.out.println(value);
        } else {
            if (context.isDeepBinding()) {
                return rootBinder.bind(name, bindable, source);
            }
        }

        return BindingResult.of((T) value.getSource());
    }

    @Override
    public <T> BindingResult<T> bind(NamePath name, Bindable<T> bindable, DataSource source) {
        DataSource collection = source.get(name);

        if (collection.isCollection()) {
            int index = 0;
            for (Object element : collection.asCollection()) {
                NamePath indexed = name.append("[" + index++ + "]");
                JavaType type    = bindable.getType().getFirst();

                System.out.println(">> type: " + type);

                BindingResult<?> result = bindValue(indexed, Bindable.of(type.getRawType()), source);

                if (result.isPresent()) {
                    System.out.println(result.getValue());
                }

            }
        }


        return BindingResult.of(null);
//        return context.getRootBinder().bind(name, bindable, source);
    }



}
