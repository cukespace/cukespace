package cucumber.runtime.arquillian;

import java.util.Collection;

import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.ServiceLoader;
import org.jboss.arquillian.test.spi.TestEnricher;
import org.jboss.arquillian.test.spi.event.suite.Before;

public class TestEnricherProvider {
    
    private static final ThreadLocal<Collection<TestEnricher>> TEST_ENRICHERS
        = new ThreadLocal<Collection<TestEnricher>>();
    
    public static Collection<TestEnricher> getTestEnrichers() {
        return TEST_ENRICHERS.get();
    }
    
    @Inject
    private Instance<ServiceLoader> serviceLoader;
    
    public void enrich(@Observes Before event) {
        Collection<TestEnricher> enrichers
            = this.serviceLoader.get().all(TestEnricher.class);
        
        TEST_ENRICHERS.set(enrichers);
    }
}
