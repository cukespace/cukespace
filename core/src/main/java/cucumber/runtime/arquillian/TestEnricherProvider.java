package cucumber.runtime.arquillian;

import java.util.Collection;

import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.ServiceLoader;
import org.jboss.arquillian.test.spi.TestEnricher;
import org.jboss.arquillian.test.spi.event.suite.Before;


/**
 * Provider for test enrichers.
 */
public class TestEnricherProvider
{
    /**
     * Thread-local storage for test enrichers.
     */
    private static final ThreadLocal<Collection<TestEnricher>> TEST_ENRICHERS
        = new ThreadLocal<Collection<TestEnricher>>();
    
    
    /**
     * Gets the test enrichers.
     * 
     * @return The test enrichers.
     */
    public static Collection<TestEnricher> getTestEnrichers()
    {
        return TEST_ENRICHERS.get();
        
    } // getTestEnrichers
    
    
    /**
     * The service loader instances.
     */
    @Inject
    private Instance<ServiceLoader> serviceLoader;
    
    
    /**
     * Initializes a new instance of the TestEnricherProvider class.
     */
    public TestEnricherProvider()
    {
        // intentionally empty
        
    } // TestEnricherProvider
    
    
    /**
     * Gets all steps enrichers and stores them.
     * 
     * @param event The before-test-execution event.
     */
    public void enrich( @Observes Before event )
    {
        Collection<TestEnricher> enrichers
            = this.serviceLoader.get().all( TestEnricher.class );
        
        TEST_ENRICHERS.set( enrichers );
        
    } // enrich
    
    
} // class TestEnricherProvider
