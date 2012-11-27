package cucumber.runtime.arquillian;

import static cucumber.runtime.arquillian.TestEnricherProvider.getTestEnrichers;
import static java.lang.String.format;

import java.util.HashMap;
import java.util.Map;
import cucumber.runtime.CucumberException;
import org.jboss.arquillian.test.spi.TestEnricher;

public class DefaultObjectFactoryExtension implements ObjectFactoryExtension {
    private final Map<Class<?>, Object> instances;

    public DefaultObjectFactoryExtension() {
        instances = new HashMap<Class<?>, Object>();
    }

    public void addClass(Class<?> clazz) {
        // intentionally empty
    }

    @Override
    public <T> T getInstance(Class<T> type) {
        if (!instances.containsKey(type)) {
            try {
                T instance = type.getConstructor().newInstance();
                for (TestEnricher testEnricher : getTestEnrichers()) {
                    testEnricher.enrich(instance);
                }
                instances.put(type, instance);
            } catch (ReflectiveOperationException exception) {
                throw new CucumberException(format("Failed to instantiate %s", type), exception);
            }
        }
        return (T) instances.get(type);
    }

    public void start() {
        // intentionally empty
    }

    @Override
    public void stop() {
        instances.clear();
    }
}
