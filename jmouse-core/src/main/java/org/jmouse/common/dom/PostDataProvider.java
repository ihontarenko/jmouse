package org.jmouse.common.dom;

import org.jmouse.common.support.context.ErrorDetails;
import org.jmouse.common.support.provider.data.DataProvider;
import org.jmouse.common.support.provider.error.ErrorProvider;

import java.util.Map;
import java.util.Set;

public class PostDataProvider implements DataProvider<String, Object>, ErrorProvider<String> {

    private final ErrorProvider<String>        errorProvider;
    private final DataProvider<String, Object> dataProvider;

    public PostDataProvider(DataProvider<String, Object> dataProvider, ErrorProvider<String> errorProvider) {
        this.errorProvider = errorProvider;
        this.dataProvider = dataProvider;
    }

    @Override
    public Object getValue(String key) {
        return dataProvider.getValue(key);
    }

    @Override
    public Map<String, Object> getValuesMap() {
        return dataProvider.getValuesMap();
    }

    @Override
    public Set<Object> getValuesSet() {
        return dataProvider.getValuesSet();
    }

    @Override
    public boolean hasError(String key) {
        return errorProvider.hasError(key);
    }

    @Override
    public ErrorDetails getError(String key) {
        return errorProvider.getError(key);
    }

    @Override
    public Map<String, ErrorDetails> getErrorsMap() {
        return errorProvider.getErrorsMap();
    }

    @Override
    public Set<ErrorDetails> getErrorsSet() {
        return errorProvider.getErrorsSet();
    }
}
