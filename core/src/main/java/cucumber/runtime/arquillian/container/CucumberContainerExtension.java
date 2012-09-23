package cucumber.runtime.arquillian.container;

import org.jboss.arquillian.container.test.spi.RemoteLoadableExtension;

import cucumber.runtime.arquillian.TestEnricherProvider;

public class CucumberContainerExtension  implements RemoteLoadableExtension {
    
    @Override
    public void register(ExtensionBuilder builder) {
        builder.observer(TestEnricherProvider.class);
    }
}
