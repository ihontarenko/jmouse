package svit.context;

abstract public class AbstractVariablesContext extends AbstractContext implements VariablesContext {

    @Override
    public <T> T getVariable(Object name) {
        return (T) getProperty(name);
    }

    @Override
    public void setVariable(Object name, Object value) {
        setProperty(name, value);
    }

}
