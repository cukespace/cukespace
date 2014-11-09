package cucumber.runtime.arquillian.client;

import cucumber.runtime.arquillian.lifecycle.CucumberLifecycle;
import cucumber.runtime.arquillian.reporter.CucumberReporter;
import cucumber.runtime.arquillian.shared.EventHelper;
import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.core.spi.LoadableExtension;

public class CucumberClientExtension implements LoadableExtension {
    @Override
    public void register(final ExtensionBuilder builder) {
        builder.service(ApplicationArchiveProcessor.class, CucumberArchiveProcessor.class)
            .observer(CucumberLifecycle.class)
            .observer(CucumberConfigurationProducer.class)
            .observer(CucumberReporter.class)
            .observer(EventHelper.class);
    }
}
