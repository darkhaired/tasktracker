package org.springframework.expression.spel.standard;

import org.springframework.asm.MethodVisitor;
import org.springframework.core.MethodParameter;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.TypedValue;
import org.springframework.expression.spel.CodeFlow;
import org.springframework.expression.spel.ExpressionState;
import org.springframework.expression.spel.SpelEvaluationException;
import org.springframework.expression.spel.SpelMessage;
import org.springframework.expression.spel.ast.SpelNodeImpl;
import org.springframework.expression.spel.support.ReflectionHelper;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public class CustomFunctionReference extends SpelNodeImpl {

    private final String name;

    // Captures the most recently used method for the function invocation *if* the method
    // can safely be used for compilation (i.e. no argument conversion is going on)
    @Nullable
    private volatile Method method;


    public CustomFunctionReference(String functionName, int pos, SpelNodeImpl... arguments) {
        super(pos, arguments);
        this.name = functionName;
    }


    @Override
    public TypedValue getValueInternal(ExpressionState state) throws EvaluationException {
        TypedValue value = state.lookupVariable(this.name);
        if (value == TypedValue.NULL) {
            throw new SpelEvaluationException(getStartPosition(), SpelMessage.FUNCTION_NOT_DEFINED, this.name);
        }

        if (!(value.getValue() instanceof Function)
                && !(value.getValue() instanceof Method) ) {
            throw new SpelEvaluationException(
                    SpelMessage.FUNCTION_REFERENCE_CANNOT_BE_INVOKED, this.name, value.getClass());
        }

        if (value.getValue() instanceof Function) {
            try {
                return executeFunctionJLRMethod(state, (Function) value.getValue());
            }
            catch (SpelEvaluationException ex) {
                ex.setPosition(getStartPosition());
                throw ex;
            }
        }

        try {
            return executeFunctionJLRMethod(state, (Method) value.getValue());
        }
        catch (SpelEvaluationException ex) {
            ex.setPosition(getStartPosition());
            throw ex;
        }

    }

    /**
     * Execute a function represented as a {@code java.lang.reflect.Method}.
     * @param state the expression evaluation state
     * @param method the method to invoke
     * @return the return value of the invoked Java method
     * @throws EvaluationException if there is any problem invoking the method
     */
    private TypedValue executeFunctionJLRMethod(ExpressionState state, Method method) throws EvaluationException {
        Object[] functionArgs = getArguments(state);

        if (!method.isVarArgs()) {
            int declaredParamCount = method.getParameterCount();
            if (declaredParamCount != functionArgs.length) {
                throw new SpelEvaluationException(SpelMessage.INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNCTION,
                        functionArgs.length, declaredParamCount);
            }
        }
        if (!Modifier.isStatic(method.getModifiers())) {
            throw new SpelEvaluationException(getStartPosition(),
                    SpelMessage.FUNCTION_MUST_BE_STATIC, ClassUtils.getQualifiedMethodName(method), this.name);
        }

        // Convert arguments if necessary and remap them for varargs if required
        TypeConverter converter = state.getEvaluationContext().getTypeConverter();
        boolean argumentConversionOccurred = ReflectionHelper.convertAllArguments(converter, functionArgs, method);
        if (method.isVarArgs()) {
            functionArgs = ReflectionHelper.setupArgumentsForVarargsInvocation(
                    method.getParameterTypes(), functionArgs);
        }
        boolean compilable = false;

        try {
            ReflectionUtils.makeAccessible(method);
            Object result = method.invoke(method.getClass(), functionArgs);
            compilable = !argumentConversionOccurred;
            return new TypedValue(result, new TypeDescriptor(new MethodParameter(method, -1)).narrow(result));
        }
        catch (Exception ex) {
            throw new SpelEvaluationException(getStartPosition(), ex, SpelMessage.EXCEPTION_DURING_FUNCTION_CALL,
                    this.name, ex.getMessage());
        }
        finally {
            if (compilable) {
                this.exitTypeDescriptor = CodeFlow.toDescriptor(method.getReturnType());
                this.method = method;
            }
            else {
                this.exitTypeDescriptor = null;
                this.method = null;
            }
        }
    }

    private TypedValue executeFunctionJLRMethod(ExpressionState state, Function method) throws EvaluationException {
        Object[] functionArgs = getArguments(state);

        // Convert arguments if necessary and remap them for varargs if required
        TypeConverter converter = state.getEvaluationContext().getTypeConverter();
        boolean compilable = false;

        try {
            Object result = method.apply(functionArgs);
            return new TypedValue(result);
        }
//        catch (ReportsException.NoSuchMetricException
//                | ReportsException.NoSucceededTasksException
//                | ReportsException.NoSoManyTasksCountException
//                | ReportsException.NoTasksForPeriodException ex1) {
//            throw ex1;
//        }
//        catch (ReportsException.InvalidArgumentsNumberException ex2) {
//            throw ex2;
//        }
        catch (Exception ex) {
            throw new SpelEvaluationException(getStartPosition(), ex, SpelMessage.EXCEPTION_DURING_FUNCTION_CALL,
                    this.name, ex.getMessage());
        }
        finally {
            if (compilable) {
            }
            else {
                this.exitTypeDescriptor = null;
                this.method = null;
            }
        }
    }

    private TypedValue executeFunctionJLRMethod(ExpressionState state, BiFunction method) throws EvaluationException {
        Object[] functionArgs = getArguments(state);

        // Convert arguments if necessary and remap them for varargs if required
        TypeConverter converter = state.getEvaluationContext().getTypeConverter();

        boolean compilable = false;

        try {
            Object result = method.apply(functionArgs[0], functionArgs[1]);
            return new TypedValue(result);
        }
        catch (Exception ex) {
            throw new SpelEvaluationException(getStartPosition(), ex, SpelMessage.EXCEPTION_DURING_FUNCTION_CALL,
                    this.name, ex.getMessage());
        }
        finally {
            if (compilable) {
            }
            else {
                this.exitTypeDescriptor = null;
                this.method = null;
            }
        }
    }

    @Override
    public String toStringAST() {
        StringBuilder sb = new StringBuilder("#").append(this.name);
        sb.append("(");
        for (int i = 0; i < getChildCount(); i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(getChild(i).toStringAST());
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Compute the arguments to the function, they are the children of this expression node.
     * @return an array of argument values for the function call
     */
    private Object[] getArguments(ExpressionState state) throws EvaluationException {
        // Compute arguments to the function
        Object[] arguments = new Object[getChildCount()];
        for (int i = 0; i < arguments.length; i++) {
            arguments[i] = this.children[i].getValueInternal(state).getValue();
        }
        return arguments;
    }

    @Override
    public boolean isCompilable() {
        Method method = this.method;
        if (method == null) {
            return false;
        }
        int methodModifiers = method.getModifiers();
        if (!Modifier.isStatic(methodModifiers) || !Modifier.isPublic(methodModifiers) ||
                !Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
            return false;
        }
        for (SpelNodeImpl child : this.children) {
            if (!child.isCompilable()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void generateCode(MethodVisitor mv, CodeFlow cf) {
        Method method = this.method;
        Assert.state(method != null, "No method handle");
        String classDesc = method.getDeclaringClass().getName().replace('.', '/');
        generateCodeForArguments(mv, cf, method, this.children);
        mv.visitMethodInsn(INVOKESTATIC, classDesc, method.getName(),
                CodeFlow.createSignatureDescriptor(method), false);
        cf.pushDescriptor(this.exitTypeDescriptor);
    }

}