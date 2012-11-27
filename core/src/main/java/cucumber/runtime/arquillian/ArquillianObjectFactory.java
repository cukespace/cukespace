package cucumber.runtime.arquillian;

import static cucumber.runtime.arquillian.TestEnricherProvider.getTestEnrichers;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.ServiceLoader;
import cucumber.runtime.CucumberException;
import cucumber.runtime.java.ObjectFactory;
import org.jboss.arquillian.test.spi.TestEnricher;

public class ArquillianObjectFactory implements ObjectFactory {
    private final Map<Class<?>, Object> instances;
    private final ObjectFactoryExtension extension;

    public ArquillianObjectFactory() {
        instances = new HashMap<Class<?>, Object>();
        Iterator<ObjectFactoryExtension> iterator = ServiceLoader.load(ObjectFactoryExtension.class).iterator();
        if (iterator.hasNext()) {
            extension = iterator.next();
            if (iterator.hasNext()) {
                throw new CucumberException("Multiple implementations of ObjectFactoryExtension found");
            }
        } else {
            extension = new DefaultObjectFactoryExtension();
        }
    }

    @Override
    public void start() {
        extension.start();
    }

    @Override
    public void stop() {
        extension.stop();
        instances.clear();
    }

    @Override
    public void addClass(Class<?> clazz) {
        extension.addClass(clazz);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getInstance(Class<T> type) {
        if (!instances.containsKey(type)) {
            T instance = extension.getInstance(type);
            for (TestEnricher testEnricher : getTestEnrichers()) {
                testEnricher.enrich(instance);
            }
            instances.put(type, instance);
        }
        return (T) instances.get(type);
    }
}
