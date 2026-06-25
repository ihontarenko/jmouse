package org.jmouse.el.node.expression;

import org.jmouse.core.access.descriptor.Describer;
import org.jmouse.core.access.descriptor.FieldDescriptor;
import org.jmouse.core.access.descriptor.structured.ObjectDescriptor;
import org.jmouse.core.context.beans.BeanLookupContext;
import org.jmouse.core.invoke.*;
import org.jmouse.core.reflection.Reflections;
import org.jmouse.el.evaluation.EvaluationContext;
import org.jmouse.el.node.AbstractExpression;
import org.jmouse.el.node.Expression;

public class BeanAccessNode extends AbstractExpression {

    private static final InvocationMethodContext EMPTY_CONTEXT = new InvocationMethodContext();

    private AccessType type;
    private String     bean;
    private BeanAction action;

    public String getBean() {
        return bean;
    }

    public void setBean(String bean) {
        this.bean = bean;
    }

    public AccessType getType() {
        return type;
    }

    public void setType(AccessType type) {
        this.type = type;
    }

    public BeanAction getAction() {
        return action;
    }

    public void setAction(BeanAction action) {
        this.action = action;
    }

    @Override
    public Object evaluate(EvaluationContext context) {
        Object bean      = null;
        Object evaluated = null;

        if (context instanceof BeanLookupContext lookupContext) {
            bean = lookupContext.getBean(getBean(), Object.class);
        }

        if (bean != null) {
            ObjectDescriptor<?> descriptor = Describer.forObjectDescriptor(bean.getClass());

            evaluated = switch (getAction()) {
                case ConstantAccess(String constant) -> {
                    FieldDescriptor fieldDescriptor = descriptor.getType().getField(constant);

                    if (fieldDescriptor == null) {
                        throw new BeanAccessException("No constant '"+ constant +"' exist in bean '"+ descriptor.getType() +"'");
                    }

                    yield Reflections.getFieldValue(null, fieldDescriptor.unwrap());
                }
                case FieldAccess(String fieldName) -> {
                    FieldDescriptor fieldDescriptor = descriptor.getType().getField(fieldName);

                    if (fieldDescriptor == null) {
                        throw new BeanAccessException("No field '$"+ fieldName +"' exist in bean '"+ descriptor.getType() +"'");
                    }

                    yield Reflections.getFieldValue(bean, fieldDescriptor.unwrap());
                }
                case MethodCall(String method, Expression arguments) -> {
                    Object   evaluatedArguments = arguments.evaluate(context);
                    Object[] arrayArguments     = new Object[0];

                    if (evaluatedArguments instanceof Object[] array) {
                        arrayArguments = array;
                    }

                    MethodInvoker       invoker         = new MethodInvoker.Default(
                            new ArrayArgumentsMethodArgumentResolver(arrayArguments));
                    InvocableMethod     invocableMethod = new InvocableMethod(
                            bean, descriptor.getType().getMethod(method).unwrap());

                    yield invoker.invoke(new InvocationRequest.Default(
                            invocableMethod, EMPTY_CONTEXT
                    ));
                }
                default -> null;
            };
        }

        return evaluated;
    }

    public enum AccessType {
        METHOD_CALL, FIELD_ACCESS, CONSTANT_ACCESS
    }

    public interface BeanAction {
    }

    public record MethodCall(String name, Expression arguments) implements BeanAction {
    }

    public record ConstantAccess(String name) implements BeanAction {
    }

    public record FieldAccess(String name) implements BeanAction {
    }

}
