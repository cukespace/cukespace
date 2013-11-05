package cucumber.runtime.arquillian.backend;

import cucumber.api.Scenario;
import cucumber.runtime.CucumberException;
import cucumber.runtime.HookDefinition;
import cucumber.runtime.MethodFormat;
import cucumber.runtime.Utils;
import gherkin.TagExpression;
import gherkin.formatter.model.Tag;

import java.lang.reflect.Method;
import java.util.Collection;

import static java.util.Arrays.asList;

public class ArquillianHookDefinition implements HookDefinition {
    private final Method method;
    private final long timeout;
    private final TagExpression tagExpression;
    private final int order;
    private final Object instance;

    public ArquillianHookDefinition(final Method method, final String[] tagExpressions,
                                    final int order, final long timeout, final Object instance) {
        this.method = method;
        this.timeout = timeout;
        this.tagExpression = new TagExpression(asList(tagExpressions));
        this.order = order;
        this.instance = instance;
    }

    @Override
    public String getLocation(final boolean detail) {
        final MethodFormat format = detail ? MethodFormat.FULL : MethodFormat.SHORT;
        return format.format(method);
    }

    @Override
    public void execute(final Scenario scenario) throws Throwable {
        Object[] args;
        switch (method.getParameterTypes().length) {
            case 0:
                args = new Object[0];
                break;
            case 1:
                if (!Scenario.class.equals(method.getParameterTypes()[0])) {
                    throw new CucumberException("When a hook declares an argument it must be of type " + Scenario.class.getName() + ". " + method.toString());
                }
                args = new Object[]{scenario};
                break;
            default:
                throw new CucumberException("Hooks must declare 0 or 1 arguments. " + method.toString());
        }

        Utils.invoke(instance, method, timeout, args);
    }

    @Override
    public boolean matches(final Collection<Tag> tags) {
        return tagExpression.evaluate(tags);
    }

    @Override
    public int getOrder() {
        return order;
    }
}
