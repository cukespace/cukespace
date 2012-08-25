package cucumber.runtime.arquillian.container;

import org.jboss.arquillian.container.test.spi.RemoteLoadableExtension;

import cucumber.runtime.arquillian.TestEnricherProvider;

/**
 * Container extension.
 */
public class CucumberContainerExtension  implements RemoteLoadableExtension {
    
    /**
     * Initializes a new instance of the CucumberContainerExtension class.
     */
    public CucumberContainerExtension() {
        
        // intentionally empty
    }
    
    @Override
    public void register(ExtensionBuilder builder) {
        
        builder.observer(TestEnricherProvider.class);
    }
}
