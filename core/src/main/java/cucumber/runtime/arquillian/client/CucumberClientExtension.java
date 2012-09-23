package cucumber.runtime.arquillian.client;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.core.spi.LoadableExtension;

import cucumber.runtime.arquillian.TestEnricherProvider;

public class CucumberClientExtension implements LoadableExtension {
    
    @Override
    public void register(ExtensionBuilder builder) {
        builder.service(ApplicationArchiveProcessor.class, CucumberArchiveProcessor.class)
            .observer(TestEnricherProvider.class);
    }
}
