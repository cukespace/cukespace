package cucumber.runtime.arquillian.backend;

import cucumber.runtime.JdkPatternArgumentMatcher;
import cucumber.runtime.MethodFormat;
import cucumber.runtime.ParameterInfo;
import cucumber.runtime.StepDefinition;
import cucumber.runtime.Utils;
import gherkin.I18n;
import gherkin.formatter.Argument;
import gherkin.formatter.model.Step;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.regex.Pattern;

public class ArquillianStepDefinition implements StepDefinition {
    private final Method method;
    private final Pattern pattern;
    private final long timeout;
    private final JdkPatternArgumentMatcher argumentMatcher;
    private final Object instance;
    private List<ParameterInfo> parameterInfos;

    public ArquillianStepDefinition(final Method method, final Pattern pattern, final long timeout, final Object instance) {
        this.method = method;
        this.parameterInfos = ParameterInfo.fromMethod(method);
        this.pattern = pattern;
        this.argumentMatcher = new JdkPatternArgumentMatcher(pattern);
        this.timeout = timeout;
        this.instance = instance;
    }

    public void execute(final I18n i18n, final Object[] args) throws Throwable {
        Utils.invoke(instance, method, timeout, args);
    }

    public List<Argument> matchedArguments(final Step step) {
        return argumentMatcher.argumentsFrom(step.getName());
    }

    public String getLocation(final boolean detail) {
        MethodFormat format = detail ? MethodFormat.FULL : MethodFormat.SHORT;
        return format.format(method);
    }

    @Override
    public Integer getParameterCount() {
        return parameterInfos.size();
    }

    @Override
    public ParameterInfo getParameterType(final int n, final Type argumentType) {
        return parameterInfos.get(n);
    }

    public boolean isDefinedAt(final StackTraceElement e) {
        return e.getClassName().equals(method.getDeclaringClass().getName()) && e.getMethodName().equals(method.getName());
    }

    @Override
    public String getPattern() {
        return pattern.pattern();
    }
}
