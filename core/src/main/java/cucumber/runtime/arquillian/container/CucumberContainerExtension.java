package cucumber.runtime.arquillian.container;

import cucumber.runtime.arquillian.lifecycle.CucumberLifecycle;
import org.jboss.arquillian.container.test.spi.RemoteLoadableExtension;

public class CucumberContainerExtension  implements RemoteLoadableExtension {
    @Override
    public void register(final ExtensionBuilder builder) {
        builder.observer(CucumberLifecycle.class);
    }
}
