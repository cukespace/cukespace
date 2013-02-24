package cucumber.runtime.arquillian.client;

import cucumber.runtime.arquillian.lifecycle.CucumberLifecycle;
import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.core.spi.LoadableExtension;

public class CucumberClientExtension implements LoadableExtension {
    
    @Override
    public void register(final ExtensionBuilder builder) {
        builder.service(ApplicationArchiveProcessor.class, CucumberArchiveProcessor.class)
            .observer(CucumberLifecycle.class);
    }
}
