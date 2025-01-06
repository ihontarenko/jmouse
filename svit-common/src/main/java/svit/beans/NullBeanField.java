package svit.beans;

public class NullBeanField implements BeanField {

    @Override
    public String getName() {
        return null;
    }

    @Override
    public FieldAccessor getFieldAccessor() {
        return new NullFieldAccessor();
    }

}
