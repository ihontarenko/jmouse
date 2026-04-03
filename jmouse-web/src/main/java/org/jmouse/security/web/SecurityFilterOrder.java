package org.jmouse.security.web;

import jakarta.servlet.Filter;
import org.jmouse.context.BeanProperties;
import org.jmouse.security.web.access.ExceptionTranslationFilter;
import org.jmouse.security.web.authentication.AnonymousAuthenticationFilter;
import org.jmouse.security.web.authentication.form.SubmitFormRequestAuthenticationFilter;
import org.jmouse.security.web.authentication.LogoutAuthenticationFilter;
import org.jmouse.security.web.authentication.www.BasicAuthenticationFilter;
import org.jmouse.security.web.authorization.AuthorizationFilter;
import org.jmouse.security.web.context.SecurityContextPersistenceFilter;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 🧱 Defines recommended filter ordering in the jMouse Security web pipeline.
 *
 * <p>Typical order (Spring-inspired):</p>
 * <ul>
 *   <li>050 ➡️ {@link SecurityContextPersistenceFilter}</li>
 *   <li>150 ➡️ {@link SubmitFormRequestAuthenticationFilter} login mechanisms</li>
 *   <li>170 ➡️ {@link LogoutAuthenticationFilter} logout mechanisms</li></li>
 *   <li>180 ➡️ {@link AnonymousAuthenticationFilter} Anonymous authentication</li>
 *   <li>180 ➡️ {@link ExceptionTranslationFilter} Exception translation</li>
 *   <li>200 ➡️ {@link AuthorizationFilter}</li>
 * </ul>
 *
 * ⚙️ Stores filters as a {@link Map} of class name → order index.
 */
@BeanProperties("jmouse.web.security.filter")
public class SecurityFilterOrder {

    /** 📑 Registered filter orders (class name → index). */
    private Map<String, Integer> orders = new LinkedHashMap<>();

    {
        setOrder(SecurityContextPersistenceFilter.class, 50);
        setOrder(BasicAuthenticationFilter.class, 120);
        setOrder(SubmitFormRequestAuthenticationFilter.class, 150);
        setOrder(LogoutAuthenticationFilter.class, 170);
        setOrder(AnonymousAuthenticationFilter.class, 180);
        setOrder(AuthorizationFilter.class, 190);
        setOrder(ExceptionTranslationFilter.class, 200);
    }

    /**
     * 📦 Get all filter orders.
     * @return mapping of filter class name → order
     */
    public Map<String, Integer> getOrders() {
        return orders;
    }

    /**
     * 📦 Replace all filter orders.
     * @param orders new mapping
     */
    public void setOrders(Map<String, Integer> orders) {
        this.orders = orders;
    }

    /**
     * 🔍 Lookup order for given filter class.
     * @param filter filter class
     * @return order index, or -1 if not found
     */
    public int getOrder(Class<?> filter) {
        return getOrder(filter.getName());
    }

    /**
     * ➕ Register order for filter class.
     * @param filter filter class
     * @param order order index
     */
    public void setOrder(Class<? extends Filter> filter, int order) {
        setOrder(filter.getName(), order);
    }

    /**
     * ➕ Register order for filter by name.
     * @param filter filter class name
     * @param order order index
     */
    public void setOrder(String filter, int order) {
        orders.put(filter, order);
    }

    /**
     * 🔍 Lookup order for filter by name.
     * @param filter filter class name
     * @return order index, or -1 if not found
     */
    public int getOrder(String filter) {
        Integer order = orders.get(filter);
        return order == null ? -1 : order;
    }

}
