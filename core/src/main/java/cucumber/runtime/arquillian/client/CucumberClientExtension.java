package cucumber.runtime.arquillian.client;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.core.spi.LoadableExtension;

import cucumber.runtime.arquillian.TestEnricherProvider;

/**
 * Cucumber client extension.
 */
public class CucumberClientExtension implements LoadableExtension {
    
    /**
     * Initializes a new instance of the CucumberClientExtension class.
     */
    public CucumberClientExtension() {
        
        // intentionally empty
    }
    
    @Override
    public void register(ExtensionBuilder builder) {
        
        builder.service(ApplicationArchiveProcessor.class, CucumberArchiveProcessor.class)
            .observer(TestEnricherProvider.class);
    }
}
