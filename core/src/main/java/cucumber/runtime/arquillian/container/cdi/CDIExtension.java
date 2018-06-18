package cucumber.runtime.arquillian.container.cdi;

import cucumber.runtime.arquillian.client.CucumberArchiveProcessor;
import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.core.spi.LoadableExtension;

public class CDIExtension implements LoadableExtension {

    @Override
    public void register(ExtensionBuilder builder) {
        builder.override(ApplicationArchiveProcessor.class, CucumberArchiveProcessor.class, CDIArchiveProcessor.class)
                .observer(CDIConfigurationObserver.class);
    }

}
