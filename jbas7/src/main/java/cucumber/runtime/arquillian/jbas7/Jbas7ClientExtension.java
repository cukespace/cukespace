package cucumber.runtime.arquillian.jbas7;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.core.spi.LoadableExtension;

/**
 * JBoss AS 7 client extension.
 */
public class Jbas7ClientExtension implements LoadableExtension {
    
    /**
     * Initializes a new instance of the Jbas7ClientExtension class.
     */
    public Jbas7ClientExtension() {
        // intentionally empty
    }
    
    @Override
    public void register(ExtensionBuilder builder) {
        builder.service(ApplicationArchiveProcessor.class, Jbas7ArchiveProcessor.class);
    }
}
