package cucumber.runtime.arquillian.container.cdi;

import cucumber.runtime.arquillian.config.CucumberConfiguration;
import cucumber.runtime.arquillian.container.CukeSpaceCDIObjectFactory;
import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;

public class CDIConfigurationObserver {
    @Inject
    private Instance<CucumberConfiguration> configuration;
    
    public void findConfiguration(final @Observes(precedence = -10) ArquillianDescriptor descriptor) {
        CucumberConfiguration cucumberConfiguration = configuration.get();
        String objectFactory = cucumberConfiguration.getObjectFactory();
        
        if ("cdi".equalsIgnoreCase(objectFactory)) {
            cucumberConfiguration.setObjectFactory(CukeSpaceCDIObjectFactory.class.getCanonicalName());
        }
    }
}
