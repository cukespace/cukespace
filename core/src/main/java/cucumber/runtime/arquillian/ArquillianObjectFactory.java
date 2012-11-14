package cucumber.runtime.arquillian;

import static cucumber.runtime.arquillian.TestEnricherProvider.getTestEnrichers;
import static java.lang.String.format;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import cucumber.runtime.CucumberException;
import cucumber.runtime.java.ObjectFactory;
import org.jboss.arquillian.test.spi.TestEnricher;

public class ArquillianObjectFactory implements ObjectFactory {
    private final Set<Class<?>> classes = new HashSet<Class<?>>();
    private final Map<Class<?>, Object> instances = new HashMap<Class<?>, Object>();

    public void addClass(Class<?> clazz) {
        classes.add(clazz);
    }

    @Override
    public <T> T getInstance(Class<T> type) {
        T instance = (T) instances.get(type);
        if (instance == null) {
            instance = cacheNewInstance(type);
        }
        return instance;
    }

    public void start() {
        // intentionally empty
    }

    @Override
    public void stop() {
        instances.clear();
    }

    private <T> T cacheNewInstance(Class<T> type) {
        try {
            Constructor<T> constructor = type.getConstructor();
            T instance = constructor.newInstance();
            for (TestEnricher testEnricher : getTestEnrichers()) {
                testEnricher.enrich(instance);
            }
            instances.put(type, instance);
            return instance;
        } catch (NoSuchMethodException e) {
            throw new CucumberException(format("%s doesn't have an empty constructor", type), e);
        } catch (Exception e) {
            throw new CucumberException(format("Failed to instantiate %s", type), e);
        }
    }
}
