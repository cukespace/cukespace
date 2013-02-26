package cucumber.runtime.arquillian.client;

import cucumber.runtime.arquillian.config.CucumberConfiguration;
import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.config.descriptor.api.ExtensionDef;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.ApplicationScoped;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

public class CucumberConfigurationProducer {
    @Inject @ApplicationScoped
    private InstanceProducer<CucumberConfiguration> configurationProducer;

    public void findConfiguration(final @Observes ArquillianDescriptor descriptor) {
        final ExtensionDef cucumberDef = descriptor.extension("cucumber");
        configurationProducer.set(CucumberConfiguration.from(cucumberDef.getExtensionProperties()));
    }
}
