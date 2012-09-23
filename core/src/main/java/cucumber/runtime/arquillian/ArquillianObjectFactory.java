package cucumber.runtime.arquillian;

import static cucumber.runtime.arquillian.TestEnricherProvider.getTestEnrichers;

import org.jboss.arquillian.test.spi.TestEnricher;

import cucumber.fallback.runtime.java.DefaultJavaObjectFactory;

public class ArquillianObjectFactory extends DefaultJavaObjectFactory {
    
    @Override
    public <T> T getInstance(Class<T> type) {
        T instance = super.getInstance(type);
        
        for (TestEnricher testEnricher : getTestEnrichers()) {
            testEnricher.enrich(instance);
        }
        
        return instance;
    }
}
