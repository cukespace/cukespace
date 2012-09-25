package cucumber.runtime.arquillian;

import static cucumber.runtime.arquillian.TestEnricherProvider.getTestEnrichers;

import java.util.HashSet;
import java.util.Set;

import org.jboss.arquillian.test.spi.TestEnricher;

import cucumber.fallback.runtime.java.DefaultJavaObjectFactory;

public class ArquillianObjectFactory extends DefaultJavaObjectFactory {

    private final Set<Class<?>> enrichedTypes;

    public ArquillianObjectFactory() {
        enrichedTypes = new HashSet<Class<?>>();
    }

    @Override
    public <T> T getInstance(Class<T> type) {
        T instance = super.getInstance(type);

        if (!enrichedTypes.contains(type)) {
            enrichedTypes.add(type);

            for (TestEnricher testEnricher : getTestEnrichers()) {
                testEnricher.enrich(instance);
            }
        }

        return instance;
    }

    @Override
    public void stop() {
        super.stop();
        enrichedTypes.clear();
    }
}
