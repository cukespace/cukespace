package cucumber.runtime.arquillian.jbas7;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.core.spi.LoadableExtension;

public class Jbas7ClientExtension implements LoadableExtension {
    
    @Override
    public void register(ExtensionBuilder builder) {
        builder.service(ApplicationArchiveProcessor.class, Jbas7ArchiveProcessor.class);
    }
}
