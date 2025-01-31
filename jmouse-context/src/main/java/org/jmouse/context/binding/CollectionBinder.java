package org.jmouse.context.binding;

abstract public class CollectionBinder extends AbstractBinder {

    public CollectionBinder(BindContext context) {
        super(context);
    }

    @Override
    public <T> BindingResult<T> bindValue(NamePath name, Bindable<T> bindable, DataSource source) {
        ObjectBinder rootBinder = context.getRootBinder();

        if (context.isDeepBinding()) {
            return rootBinder.bind(name, bindable, source);
        }

        return BindingResult.of((T) source.get(name).getSource());
    }

    @Override
    public <T> BindingResult<T> bind(NamePath name, Bindable<T> bindable, DataSource source) {
        DataSource collection = source.get(name);

        if (collection.isCollection()) {
            int index = 0;
            for (Object element : collection.asCollection()) {
                NamePath   indexed = name.append("[" + index++ + "]");
                BindingResult<?> result = bindValue(indexed, Bindable.of(bindable.getType().getFirst().getRawType()), source);

                if (result.isPresent()) {
                    System.out.println(result.getValue());
                }

            }
        }


        return BindingResult.of(null);
//        return context.getRootBinder().bind(name, bindable, source);
    }



}
