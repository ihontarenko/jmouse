package org.jmouse.settings.spring;

import org.jmouse.settings.ParameterStore;
import org.jmouse.settings.SettingHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

/** 🎚️ The settings' rows, each read and each change one transaction: what the library leaves to its caller, said once here. */
public class TransactionalParameterStore implements ParameterStore {

    private final ParameterStore rows;

    public TransactionalParameterStore(ParameterStore rows) {
        this.rows = rows;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> kept(SettingHolder holder, String owner, String section) {
        return rows.kept(holder, owner, section);
    }

    @Override
    @Transactional
    public void keep(SettingHolder holder, String owner, String section, Map<String, String> values) {
        rows.keep(holder, owner, section, values);
    }

    @Override
    @Transactional
    public void forget(SettingHolder holder, String owner, String section, Set<String> keys) {
        rows.forget(holder, owner, section, keys);
    }

    @Override
    @Transactional
    public void replace(SettingHolder holder, String owner, String section, Map<String, String> values) {
        rows.replace(holder, owner, section, values);
    }
}
